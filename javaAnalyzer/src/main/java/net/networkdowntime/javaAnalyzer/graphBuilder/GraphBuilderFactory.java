package net.networkdowntime.javaAnalyzer.graphBuilder;

import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.DiagramType;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;

public class GraphBuilderFactory {

	public static String createGraph(Project project, JavaFilter filter) {
		GraphBuilder builder;
		if (filter.getDiagramType() == DiagramType.PACKAGE_DIAGRAM) {
			builder = new PackageGraphBuilder();
		} else if (filter.getDiagramType() == DiagramType.CLASS_ASSOCIATION_DIAGRAM) {
			builder = new ClassGraphBuilder();
		} else {
			builder = new UnreferencedGraphBuilder();
		}
		
		return builder.createGraph(project, filter);
	}
}
