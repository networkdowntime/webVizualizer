package net.networkdowntime.dbAnalyzer.erdiagrams;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.dbAnalyzer.erdiagrams.database.DatabaseAbstraction;

public class DatabaseWalker {

	boolean debugOutput = false;
	// Connection conn;
//	QueryRunner run;
	DatabaseAbstraction dbAbstraction;

	Map<String, Schema> schemas = new HashMap<String, Schema>();
	List<String> schemasToScan;

	public DatabaseWalker(DatabaseAbstraction dbAbstraction, String[] schemasToScan) {
		this.dbAbstraction = dbAbstraction;
		this.schemasToScan = new ArrayList<String>();
		for (String schema : schemasToScan)
			this.schemasToScan.add(schema);
	}

	public DatabaseWalker(DatabaseAbstraction dbAbstraction, List<String> schemasToScan) {
		this.dbAbstraction = dbAbstraction;
		this.schemasToScan = schemasToScan;
	}

	public void startWalking() {
		getTableNames();
		getTableColumns();
		getTableConstrints();
		followTableConstrints();
	}

	private void getTableNames() {
		schemas = dbAbstraction.getTableNames(schemasToScan);
	}

	private void getTableColumns() {
		try {
			dbAbstraction.getTableColumns(schemas);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getTableConstrints() {
			dbAbstraction.getTableConstrints(schemas);
	}

	private void followTableConstrints() {
		dbAbstraction.followTableConstrints(schemas);
	}

}
