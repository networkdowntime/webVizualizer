package net.networkdowntime.javaAnalyzer.graphBuilder;

import net.networkdowntime.javaAnalyzer.javaModel.Block;
import net.networkdowntime.javaAnalyzer.javaModel.Class;
import net.networkdowntime.javaAnalyzer.javaModel.Method;
import net.networkdowntime.javaAnalyzer.javaModel.ModelVisitor;
import net.networkdowntime.javaAnalyzer.javaModel.Package;
import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;

public class UnreferencedGraphBuilder extends ModelVisitor implements GraphBuilder {
	private JavaFilter filter;
	private StringBuilder graph = new StringBuilder();

	@Override
	public void visit(Block visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Class visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Method visitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Package visitor) {

	}

	public String createGraph(Project project, JavaFilter filter) {
		this.filter = filter;
		this.visit(project);
		return graph.toString();
	}

}
