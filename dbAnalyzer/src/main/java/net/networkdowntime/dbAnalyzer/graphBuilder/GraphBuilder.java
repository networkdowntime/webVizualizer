package net.networkdowntime.dbAnalyzer.graphBuilder;

import net.networkdowntime.dbAnalyzer.dbModel.DatabaseWalker;
import net.networkdowntime.dbAnalyzer.viewFilter.GraphFilter;

public interface GraphBuilder {

	public String createGraph(DatabaseWalker dbWalker, GraphFilter filter);

}
