package net.networkdowntime.dbAnalyzer.graphBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.networkdowntime.dbAnalyzer.dbModel.Column;
import net.networkdowntime.dbAnalyzer.dbModel.Constraint;
import net.networkdowntime.dbAnalyzer.dbModel.ConstraintType;
import net.networkdowntime.dbAnalyzer.dbModel.DatabaseWalker;
import net.networkdowntime.dbAnalyzer.dbModel.Schema;
import net.networkdowntime.dbAnalyzer.dbModel.Table;
import net.networkdowntime.dbAnalyzer.viewFilter.GraphFilter;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class ERDiagramCreator implements GraphBuilder {
	private static final Logger LOGGER = LogManager.getLogger(ERDiagramCreator.class.getName());

	private GraphvizRenderer renderer;
	private StringBuilder graph = new StringBuilder();
	private List<String> edgeList = new ArrayList<String>();

	public ERDiagramCreator() {
	}

	public String createGraph(DatabaseWalker dbWalker, GraphFilter filter) {
		renderer = new GraphvizDotRenderer();
		graph.append(renderer.getHeader());

		TreeSet<String> sortedTables = new TreeSet<String>((String s1, String s2) -> s1.compareTo(s2));
		sortedTables.addAll(filter.getTablesToInclude());

		String prevSchemaName = null;
		for (String canonicalTable : sortedTables) {
			String[] explodedCanonicalTable = canonicalTable.split("\\.");
			String url = explodedCanonicalTable[0];
			String schemaName = explodedCanonicalTable[1];
			String tableName = explodedCanonicalTable[2];

			if (prevSchemaName != null && !schemaName.equals(prevSchemaName)) { // changed schemas and not first time through, close cluster
				graph.append(renderer.getEndCluster());
			}

			Schema schema = dbWalker.getSchema(url, schemaName);
			if (schema != null) {
				Table table = schema.getTables().get(tableName);
				if (table != null && !filter.skipTable(url, schema, table)) {
					if (!schemaName.equals(prevSchemaName)) {
						graph.append(renderer.getBeginCluster(schemaName));
						graph.append(renderer.getLabel(schemaName));
					}
					graph.append(analyzeTable(url, schemaName, table, filter));
				}
			}
			prevSchemaName = schemaName;
		}
		graph.append(renderer.getEndCluster()); // close the last cluster

		for (String edge : edgeList) {
			graph.append(edge);
		}

		graph.append(renderer.getFooter());
		return graph.toString();

	}

	public String analyzeTable(String url, String schemaName, Table table, GraphFilter filter) {
		StringBuffer sb = new StringBuffer();

		String numberOfRows = "";
		if (table.getNumberOfRows() != null) {
			numberOfRows = " (" + table.getNumberOfRows() + " rows)";
		}

		sb.append(renderer.getBeginRecord(table.getCanonicalName(), table.getName(), numberOfRows));

		int nonDisplayedColumnCount = table.getColumns().size();

		for (Column col : table.getColumns().values()) {

			boolean pk = table.isColumnPK(col);
			boolean fk = table.isColumnFK(col);

			if (filter.isShowAllColumnsOnTables() || pk || fk) {
				nonDisplayedColumnCount--;
				String cons = "";
				if (pk && fk)
					cons += " PFK";
				else if (pk)
					cons += " PK";
				else if (fk)
					cons += " FK";

				if (!col.isNullable())
					cons += " NN";

				sb.append(renderer.addRecordField(col.getName(), col.getName() + " (" + col.getColumnType() + ")" + cons));

			}
		}

		if (!filter.isShowAllColumnsOnTables()) {
			sb.append(renderer.addRecordField("otherColumns", nonDisplayedColumnCount + " Other Columns"));
		}

		sb.append(renderer.getEndRecord());

		if (filter.isConnectWithFKs()) {
			for (Constraint con : table.getConstraints().values()) {
				if (!filter.skipFKForConstraint(con) && con.getConstraintType() == ConstraintType.FOREIGN_KEY && con.getRefTable() != null) {
					LOGGER.debug("constraint type: " + con.getConstraintType());
					LOGGER.debug("table name: " + table.getName());
					LOGGER.debug("con.getRefTable() == null: " + (con.getRefTable() == null));
					if (con.getRefTable() != null)
						LOGGER.debug("con.getRefTable().getName(): " + con.getRefTable().getName());
					LOGGER.debug("constraint name: " + con.getName());

					edgeList.add(renderer.addEdge(table.getCanonicalName(), con.getRefTable().getCanonicalName(), (filter.isShowLabelsOnFKs()) ? con.getName() : ""));
				}
			}
		}

		return sb.toString();
	}

}
