package net.networkdowntime.db.erdiagrams;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.networkdowntime.db.erdiagrams.database.DatabaseAbstraction;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory;
import net.networkdowntime.db.viewFilter.GraphFilter;
import net.networkdowntime.renderer.GraphvizNeatoRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;


public class ERDiagramCreator {

	public static final String DTO_PACKAGE_PATTERN = "#schema.#table";

	private static final boolean DEBUG_OUTPUT = false;

	private DatabaseWalker dw;

	public ERDiagramCreator() {
	}

	public ERDiagramCreator(DatabaseAbstractionFactory.DBType dbType, String userName, String password, String url, String[] schemasToScan) {
		DatabaseAbstraction dbAbstraction = DatabaseAbstractionFactory.getDatabaseAbstraction(DEBUG_OUTPUT, dbType, userName, password, url);
		dw = new DatabaseWalker(dbAbstraction, schemasToScan);
		dw.startWalking();
	}

	public DatabaseAbstraction setConnection(DatabaseAbstractionFactory.DBType dbType, String userName, String password, String url) {
		DatabaseAbstraction dbAbstraction = DatabaseAbstractionFactory.getDatabaseAbstraction(DEBUG_OUTPUT, dbType, userName, password, url);
		return dbAbstraction;
	}

	public void analyzeDatabase(DatabaseAbstraction dbAbstraction, List<String> schemasToScan) {
		dw = new DatabaseWalker(dbAbstraction, schemasToScan);
		dw.startWalking();
	}
	
	public void createDiagramFile(GraphFilter filter) {

		File graphFile = new File("graphFile.gv");
		try {
			FileWriter fw = new FileWriter(graphFile);
			fw.write(createGraphvizString(filter));
			fw.close();

//			Runtime systemShell = Runtime.getRuntime();
//			Process shellOutput = systemShell.exec("\\cygwin64\\bin\\bash.exe --login -i -c \"cd '/cygdrive/c/'; pwd; /bin/neato -Tsvg -O graphFile.gv\"");
			//Process shellOutput = systemShell.exec("\\cygwin64\\bin\\bash.exe --login -i -c \"cd '/cygdrive/c/'; pwd; /bin/dot -Tsvg -O graphFile.gv\"");

//			InputStreamReader isr = new InputStreamReader(shellOutput.getInputStream());
//			BufferedReader br = new BufferedReader(isr);
//			String line = null;
//			System.out.println("<OUTPUT/>");
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//			}
//			System.out.println("</OUTPUT>");
//			int exitVal = shellOutput.waitFor();
//			System.out.println("Process Exit Value : " + exitVal);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println(erCreator.getGraph());

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ERDiagramCreator erDiagramCreator = null;

		if (args.length > 4) {
			String[] schemasToScan = new String[args.length - 4];
			for (int i = 4; i < args.length; i++)
				schemasToScan[i - 4] = args[i];
			String dbType = args[0];
			String username = args[1];
			String password = args[2];
			String url = args[3];
			erDiagramCreator = new ERDiagramCreator(DatabaseAbstractionFactory.DBType.valueOf(dbType), username, password, url, schemasToScan);
		}

//		List<String> excludeFKForColumnsNamed = new ArrayList<String>();
//		excludeFKForColumnsNamed.add("CREATED_BY");
//		excludeFKForColumnsNamed.add("UPDATED_BY");

		GraphFilter filter = new GraphFilter();
		filter.addExcludeFKForColumnsNamed("CREATED_BY");
		filter.addExcludeFKForColumnsNamed("UPDATED_BY");
		filter.setConnectWithFKs(true);
		filter.setShowAllColumnsOnTables(false);
		filter.setShowLabelsOnFKs(false);
		filter.setIncludeTablesWithMoreXRows(1);
		erDiagramCreator.createDiagramFile(filter);
	}

	public List<String> getAllScannedTables() {
		List<String> tables = new ArrayList<String>();
		if (dw.schemas != null) {
			for (String schema : dw.schemas.keySet()) {
				for (String table : dw.schemas.get(schema).getTables().keySet()) {
					tables.add(schema + "." + table);
				}
			}
		}

//		Collections.sort(tables.subList(1, tables.size()));

		return tables;
	}

	public String createGraphvizString(GraphFilter filter) {
		return getGraph(getAllScannedTables(), filter);
	}

	private String getGraph(List<String> tables, GraphFilter filter) {
		Map<String, Schema> schemas = this.dw.schemas;

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
