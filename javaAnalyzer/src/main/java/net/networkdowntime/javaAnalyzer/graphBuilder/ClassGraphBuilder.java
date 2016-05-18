package net.networkdowntime.javaAnalyzer.graphBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.Method;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.javaAnalyzer.visitors.DependencyDepthVisitor;
import net.networkdowntime.javaAnalyzer.visitors.DependencyDepthVisitor.DepthType;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class ClassGraphBuilder extends ModelVisitor implements GraphBuilder {
	private GraphvizRenderer renderer;
	private JavaFilter filter;
	private StringBuilder graph = new StringBuilder();
	private List<String> edgeList = new ArrayList<String>();

	private Map<String, Integer> searchResults = new LinkedHashMap<String, Integer>();
	private DependencyDepthVisitor dependencyVisitor = new DependencyDepthVisitor();

	public String createGraph(Project project, JavaFilter filter) {
		this.filter = filter;
		renderer = new GraphvizDotRenderer();
		graph.append(renderer.getHeader());

		project.resetSearchRank();

		String query = filter.getAdvancedSearchQuery();
		if (query != null && !query.isEmpty()) {
			// when searching by default exclude all classes
			filter.setPackagesToExclude(new HashSet<String>());
			filter.setClassesToExclude(new HashSet<String>(project.getClassNames(new ArrayList<String>())));

			// unexclude classes based on search results  
			this.searchResults = project.searchQuery(query, 10, true);
			filter.getClassesToExclude().removeAll(this.searchResults.keySet());
		}

		dependencyVisitor = new DependencyDepthVisitor(project, DepthType.CLASS, filter.getUpstreamReferenceDepth(), filter.getDownstreamDependencyDepth(), filter.getClassesToExclude(),
				filter.getPackagesToExclude());

		this.startVisiting(project);

		for (String edge : edgeList) {
			graph.append(edge);
		}

		graph.append(renderer.getFooter());

		return graph.toString();
	}

	@Override
	public void visit(Class clazz) {
		if ((!filter.getClassesToExclude().contains(clazz.getCanonicalName()) && filter.isFromFile() && clazz.isFromFile()) || !filter.isFromFile()) {
			if (clazz.getName() == null) {
				System.err.println("!!!" + clazz.getName() + ": class with null name");
			} else {
				String color;
				int red, green, blue;
				red = green = blue = 0xFF;

				int searchRank = 0;
				if (this.searchResults.containsKey(clazz.getCanonicalName())) {
					searchRank = this.searchResults.get(clazz.getCanonicalName());
				}

				int downstreamReferenceDepth = dependencyVisitor.getDownstreamReferenceDepth(clazz.getCanonicalName());
				int upstreamReferenceDepth = dependencyVisitor.getUpstreamReferenceDepth(clazz.getCanonicalName());

				if (searchRank > 0) {
					// yellow
					red = ColorUtil.getColor(0xFF, 0xFF, 10, searchRank);
					green = ColorUtil.getColor(0xFF, 0xE1, 10, searchRank);
					blue = ColorUtil.getColor(0xFF, 0x3B, 10, searchRank);
				} else if (filter.getDownstreamDependencyDepth() != null && downstreamReferenceDepth > 0 && filter.getUpstreamReferenceDepth() != null
						&& upstreamReferenceDepth > 0) {
					// red
					red = ColorUtil.getColor(0xFF, 0xEF, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
					green = ColorUtil.getColor(0xFF, 0x53, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
					blue = ColorUtil.getColor(0xFF, 0x50, 6, Math.min(upstreamReferenceDepth, downstreamReferenceDepth));
				} else if (filter.getDownstreamDependencyDepth() != null && downstreamReferenceDepth > 0) {
					// teal
					red = ColorUtil.getColor(0xFF, 0x00, 6, downstreamReferenceDepth);
					green = ColorUtil.getColor(0xFF, 0xAC, 6, downstreamReferenceDepth);
					blue = ColorUtil.getColor(0xFF, 0xC1, 6, downstreamReferenceDepth);
				} else if (filter.getUpstreamReferenceDepth() != null && upstreamReferenceDepth > 0) {
					// blue
					red = ColorUtil.getColor(0xFF, 0x03, 6, upstreamReferenceDepth);
					green = ColorUtil.getColor(0xFF, 0x9B, 6, upstreamReferenceDepth);
					blue = ColorUtil.getColor(0xFF, 0xE5, 6, upstreamReferenceDepth);
				}

				color = "#" + String.format("%06X", ColorUtil.mixColorToRGBValue(red, green, blue));

				populateClassDetails(clazz, color);
			}
		}
	}

	protected void populateClassDetails(Class clazz, String color) {
		if (clazz.isAnonymous()) {
			graph.append(renderer.getBeginRecord(clazz.getCanonicalName(), "<anonymous>\r\n" + clazz.getName(), "", color));
		} else if (clazz.isInterface()) {
			graph.append(renderer.getBeginRecord(clazz.getCanonicalName(), "<interface>\r\n" + clazz.getName(), "", color));
		} else {
			graph.append(renderer.getBeginRecord(clazz.getCanonicalName(), clazz.getName(), "", color));
		}

		if (filter.isShowFields()) {
			for (String field : clazz.getVarNameClassMap().keySet()) {
				Class referencedClass = clazz.getVarNameClassMap().get(field);
				graph.append(renderer.addRecordField(field, field + ": " + referencedClass.getName()));
			}
		}

		if (filter.isShowMethods()) {
			for (Method method : clazz.getMethods().values()) {
				graph.append(renderer.addRecordField(method.getName(), method.getName()));
			}
		}

		graph.append(renderer.getEndRecord());

		HashSet<String> refsToSkip = new HashSet<String>();

		// Add edge for extending another class, if only 1 reference to that
		// class don't add a reference edge later
		Class extendsClass = clazz.getExtnds();
		if (extendsClass != null) {
			boolean exclude = filter.getPackagesToExclude().contains(extendsClass.getPkg().getName());
			exclude = exclude || filter.getClassesToExclude().contains(extendsClass.getCanonicalName());
			if (filter.isFromFile())
				exclude = exclude || (filter.isFromFile() && !extendsClass.isFromFile());

			if (!exclude) {
				edgeList.add((String) renderer.addReversedEdge(clazz.getCanonicalName(), extendsClass.getCanonicalName(), "", true));

				Integer count = clazz.getUnresolvedClassCount().get(extendsClass.getName());
				if (count != null && count.intValue() == 1) {
					refsToSkip.add(extendsClass.getName());
				}
			}
		}

		// Add edges for implementing interfaces, if only 1 reference to
		// that class don't add a reference edge later
		for (Class intr : clazz.getImplements()) {
			boolean exclude = filter.getPackagesToExclude().contains(intr.getPkg().getName());
			exclude = exclude || filter.getClassesToExclude().contains(intr.getCanonicalName());
			if (filter.isFromFile())
				exclude = exclude || (filter.isFromFile() && !intr.isFromFile());

			if (!exclude) {
				edgeList.add((String) renderer.addReversedEdge(clazz.getCanonicalName(), intr.getCanonicalName(), "", true, false));

				Integer count = clazz.getUnresolvedClassCount().get(intr.getName());
				if (count != null && count.intValue() == 1) {
					refsToSkip.add(intr.getName());
				}
			}
		}

		for (Class dependsOnClazz : clazz.getClassDependencies().values()) {
			if (!refsToSkip.contains(dependsOnClazz.getName())) {
				boolean exclude = filter.getPackagesToExclude().contains(dependsOnClazz.getPkg().getName());
				exclude = exclude || filter.getClassesToExclude().contains(dependsOnClazz.getCanonicalName());
				if (!exclude) {
					if ((filter.isFromFile() && dependsOnClazz.isFromFile()) || !filter.isFromFile()) {
						edgeList.add((String) renderer.addEdge(clazz.getCanonicalName(), dependsOnClazz.getCanonicalName(), ""));
					}
				}
			}
		}
	}

	@Override
	public void visit(Package pkg) {
		boolean exclude = filter.getPackagesToExclude().contains(pkg.getName());

		if ((!exclude && filter.isFromFile() && pkg.isFromFile()) || !filter.isFromFile()) {
			graph.append(renderer.getBeginCluster(pkg.getName()));

			super.visit(pkg);

			graph.append(renderer.getEndCluster());
		}
	}
}
