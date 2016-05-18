package net.networkdowntime.javaAnalyzer.graphBuilder;

import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;

public class UnreferencedGraphBuilder extends ClassGraphBuilder implements GraphBuilder {

	@Override
	protected void populateClassDetails(Class clazz, String color) {
		if (clazz.getReferencedByClass().size() == 0) {
			super.populateClassDetails(clazz, color);
		}
	}

	public String createGraph(Project project, JavaFilter filter) {
		return super.createGraph(project, filter);
	}

}
