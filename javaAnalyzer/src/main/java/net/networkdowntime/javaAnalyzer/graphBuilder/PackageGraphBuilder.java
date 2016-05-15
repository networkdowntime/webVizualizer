package net.networkdowntime.javaAnalyzer.graphBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.networkdowntime.javaAnalyzer.javaModel.Block;
import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.Method;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.logger.Logger;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class PackageGraphBuilder extends ModelVisitor implements GraphBuilder {
	private GraphvizRenderer renderer;
	private JavaFilter filter;
	private StringBuilder graph = new StringBuilder();
	private List<String> edgeList = new ArrayList<String>();

	@Override
	public void visit(Block block) {
	}

	@Override
	public void visit(Class clazz) {
	}

	@Override
	public void visit(Method method) {
	}

	public String createGraph(Project project, JavaFilter filter) {
		this.filter = filter;

		project.resetReferenceDepths();
		project.resetSearchRank();

		renderer = new GraphvizDotRenderer();
		graph.append(renderer.getHeader());

		if (filter.getAdvancedSearchQuery() != null) {
			String query = filter.getAdvancedSearchQuery();
			if (!query.isEmpty()) {
				filter.setPackagesToExclude(new HashSet<String>(project.getPackageNames()));
				filter.setClassesToExclude(new HashSet<String>(new ArrayList<String>()));

				int rank = 0;
				LinkedHashMap<String, Float> results = new LinkedHashMap<String, Float>(project.searchQuery(query, 10, false));
				results = results.entrySet().stream()
						.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(x, y) -> {
									throw new AssertionError();
								},
								LinkedHashMap::new));

				for (String name : results.keySet()) {
					filter.getPackagesToExclude().remove(name);
					project.getPackage(name).setSearchRank(rank);
					rank++;
				}
			}
		}

		// Replace with test of filter for depth selection
		Integer downDepth = filter.getDownstreamDependencyDepth();
		Integer upDepth = filter.getUpstreamReferenceDepth();
		if ((downDepth != null) || (upDepth != null)) {
			project.unExcludePackagesBasedOnDepth(downDepth, upDepth, filter);
		}

		this.visit(project);

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
				graph.append(createGraph(pkg));
			}
		}
	
	}

	public String createGraph(Package pkg) {
		Logger.log(0, "Package: " + pkg.getName());

		String color;
		int red, green, blue;
		red = green = blue = 0xFF;

		if (pkg.getSearchRank() > 0) {
			// yellow
			red = ColorUtil.getColor(0xFF, 0xFF, 10, pkg.getSearchRank());
			green = ColorUtil.getColor(0xFF, 0xE1, 10, pkg.getSearchRank());
			blue = ColorUtil.getColor(0xFF, 0x3B, 10, pkg.getSearchRank());
		} else if (filter.getDownstreamDependencyDepth() != null && pkg.getDownstreamReferenceDepth() > 0 && filter.getUpstreamReferenceDepth() != null && pkg.getUpstreamReferenceDepth() > 0) {
			// red
			red = ColorUtil.getColor(0xFF, 0xEF, 6, Math.min(pkg.getUpstreamReferenceDepth(), pkg.getDownstreamReferenceDepth()));
			green = ColorUtil.getColor(0xFF, 0x53, 6, Math.min(pkg.getUpstreamReferenceDepth(), pkg.getDownstreamReferenceDepth()));
			blue = ColorUtil.getColor(0xFF, 0x50, 6, Math.min(pkg.getUpstreamReferenceDepth(), pkg.getDownstreamReferenceDepth()));

		} else if (filter.getDownstreamDependencyDepth() != null && pkg.getDownstreamReferenceDepth() > 0) {
			// teal
			red = ColorUtil.getColor(0xFF, 0x00, 6, pkg.getDownstreamReferenceDepth());
			green = ColorUtil.getColor(0xFF, 0xAC, 6, pkg.getDownstreamReferenceDepth());
			blue = ColorUtil.getColor(0xFF, 0xC1, 6, pkg.getDownstreamReferenceDepth());
		} else if (filter.getUpstreamReferenceDepth() != null && pkg.getUpstreamReferenceDepth() > 0) {
			// blue
			red = ColorUtil.getColor(0xFF, 0x03, 6, pkg.getUpstreamReferenceDepth());
			green = ColorUtil.getColor(0xFF, 0x9B, 6, pkg.getUpstreamReferenceDepth());
			blue = ColorUtil.getColor(0xFF, 0xE5, 6, pkg.getUpstreamReferenceDepth());
		}

		color = "#" + String.format("%06X", ColorUtil.mixColorToRGBValue(red, green, blue));

		StringBuffer sb = new StringBuffer();

		sb.append(renderer.getBeginRecord(pkg.getName(), pkg.getName(), "", color));
		sb.append(renderer.getEndRecord());

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

		return sb.toString();
	}

}
