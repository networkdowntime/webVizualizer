package net.networkdowntime.dbAnalyzer.graphBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstraction;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory;
import net.networkdowntime.dbAnalyzer.dbModel.Column;
import net.networkdowntime.dbAnalyzer.dbModel.Constraint;
import net.networkdowntime.dbAnalyzer.dbModel.ConstraintType;
import net.networkdowntime.dbAnalyzer.dbModel.DatabaseWalker;
import net.networkdowntime.dbAnalyzer.dbModel.Schema;
import net.networkdowntime.dbAnalyzer.dbModel.Table;
import net.networkdowntime.dbAnalyzer.viewFilter.GraphFilter;
import net.networkdowntime.renderer.GraphvizNeatoRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;

public class ERDiagramCreator implements GraphBuilder {

	private static final boolean DEBUG_OUTPUT = true;
	private DatabaseWalker dbWalker;

	public ERDiagramCreator() {
	}

	public DatabaseAbstraction setConnection(DatabaseAbstractionFactory.DBType dbType, String userName, String password, String url) {
		DatabaseAbstraction dbAbstraction = DatabaseAbstractionFactory.getDatabaseAbstraction(DEBUG_OUTPUT, dbType, userName, password, url);
		return dbAbstraction;
	}

	public void analyzeDatabase(DatabaseAbstraction dbAbstraction, List<String> schemasToScan) {
		dbWalker = new DatabaseWalker(dbAbstraction, schemasToScan);
		dbWalker.startWalking();
	}

	public List<String> getAllScannedTables() {
		List<String> tables = new ArrayList<String>();
		if (dbWalker != null && dbWalker.getSchemas() != null) {
			for (String schema : dbWalker.getSchemas().keySet()) {
				for (String table : dbWalker.getSchemas().get(schema).getTables().keySet()) {
					tables.add(schema + "." + table);
				}
			}
		}

		//		Collections.sort(tables.subList(1, tables.size()));

		return tables;
	}

	public String createGraph(DatabaseWalker dbWalker, GraphFilter filter) {
		
		return getGraph(getAllScannedTables(), filter);
	}

	private String getGraph(List<String> tables, GraphFilter filter) {
		
		Map<String, Schema> schemas = new HashMap<String, Schema>();
		if (dbWalker != null && dbWalker.getSchemas() != null) {
			schemas.putAll(dbWalker.getSchemas());
		}

		GraphvizRenderer renderer = new GraphvizNeatoRenderer();

		StringBuffer graph = new StringBuffer();
		graph.append(renderer.getHeader());

		for (String schemaName : schemas.keySet()) {
			Schema schema = schemas.get(schemaName);

			if (schemas.size() > 1) {
				graph.append(renderer.getBeginCluster(schemaName));
			}

			graph.append(renderer.getLabel(schemaName));

			for (Table table : schema.getTables().values()) {

				if (!filter.skipTable(schema, table)) {
					graph.append(analyzeTable(renderer, schemaName, table, filter));
				}

			}

			if (schemas.size() > 1) {
				graph.append(renderer.getEndCluster());
			}
		}

		graph.append(renderer.getFooter());
		return graph.toString();
	}

	public String analyzeTable(GraphvizRenderer renderer, String schemaName, Table table, GraphFilter filter) {
		StringBuffer sb = new StringBuffer();

		String numberOfRows = "";
		if (table.getNumberOfRows() != null) {
			numberOfRows = " (" + table.getNumberOfRows() + " rows)";
		}

		sb.append(renderer.getBeginRecord(table.getName(), numberOfRows));

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
					System.out.println("constraint type: " + con.getConstraintType());
					System.out.println("table name: " + table.getName());
					System.out.println("con.getRefTable() == null: " + (con.getRefTable() == null));
					if (con.getRefTable() != null)
						System.out.println("con.getRefTable().getName(): " + con.getRefTable().getName());
					System.out.println("constraint name: " + con.getName());

					sb.append(renderer.addEdge(table.getName(), con.getRefTable().getName(), (filter.isShowLabelsOnFKs()) ? con.getName() : ""));
				}
			}
		}

		return sb.toString();
	}

}
