package net.networkdowntime.javaAnalyzer.graphBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.javaAnalyzer.visitors.DependencyDepthVisitor;
import net.networkdowntime.javaAnalyzer.visitors.DependencyDepthVisitor.DepthType;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

// TODO Remove search rank from the model

public class PackageGraphBuilder extends ModelVisitor implements GraphBuilder {
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
			// when searching by default exclude all packages
			filter.setPackagesToExclude(new HashSet<String>(project.getPackageNames()));
			filter.setClassesToExclude(new HashSet<String>(new ArrayList<String>()));

			// unexclude packages based on search results  
			this.searchResults = project.searchQuery(query, 10, false);
			filter.getPackagesToExclude().removeAll(this.searchResults.keySet());
		}

		dependencyVisitor = new DependencyDepthVisitor(project, DepthType.PACKAGE, filter.getUpstreamReferenceDepth(), filter.getDownstreamDependencyDepth(), filter.getClassesToExclude(),
				filter.getPackagesToExclude());

		this.startVisiting(project);

		for (String edge : edgeList) {
			graph.append(edge);
		}

		graph.append(renderer.getFooter());

		return graph.toString();
	}

	@Override
	public void visit(Package pkg) {
		boolean exclude = filter.getPackagesToExclude().contains(pkg.getName());

		if (!exclude) {
			if ((filter.isFromFile() && pkg.isFromFile()) || !filter.isFromFile()) {
				String color;
				int red, green, blue;
				red = green = blue = 0xFF;

				int searchRank = 0;
				if (this.searchResults.containsKey(pkg.getName())) {
					searchRank = this.searchResults.get(pkg.getName());
				}

				int downstreamReferenceDepth = dependencyVisitor.getDownstreamReferenceDepth(pkg.getName());
				int upstreamReferenceDepth = dependencyVisitor.getUpstreamReferenceDepth(pkg.getName());

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

				graph.append(renderer.getBeginRecord(pkg.getName(), pkg.getName(), "", color));
				graph.append(renderer.getEndRecord());

				HashMap<String, Integer> referencedPackages = new HashMap<String, Integer>();
				for (Class c : pkg.getClasses().values()) {
					if ((filter.isFromFile() && c.isFromFile()) || !filter.isFromFile()) {
						for (Package p : c.getPackageDependencies()) {
							if ((filter.isFromFile() && p.isFromFile()) || !filter.isFromFile()) {
								Integer count = referencedPackages.get(p.getName());
								if (count == null)
									count = 0;
								referencedPackages.put(p.getName(), count);
							}
						}

						for (Class c1 : c.getClassDependencies().values()) {
							if ((filter.isFromFile() && c1.isFromFile()) || !filter.isFromFile()) {
								Integer count = referencedPackages.get(c1.getPackage().getName());
								if (count == null)
									count = 0;
								referencedPackages.put(c1.getPackage().getName(), count + 1);
							}
						}
					}
				}

				for (String pkgName : referencedPackages.keySet()) {
					if (!filter.getPackagesToExclude().contains(pkgName)) {
						Integer count = referencedPackages.get(pkgName);
						edgeList.add((String) renderer.addEdge(pkg.getName(), pkgName, count.toString(), false));
					}
				}
			}
		}
	}
}
