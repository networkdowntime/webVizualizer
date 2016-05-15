package net.networkdowntime.javaAnalyzer.graphBuilder;

import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;

public interface GraphBuilder {

	public String createGraph(Project project, JavaFilter filter);

}
