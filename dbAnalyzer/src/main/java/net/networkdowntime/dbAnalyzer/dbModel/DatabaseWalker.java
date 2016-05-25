package net.networkdowntime.dbAnalyzer.dbModel;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstraction;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory.DBType;

public class DatabaseWalker {

	private Map<String, DatabaseAbstraction> dbAbstractions = new HashMap<String, DatabaseAbstraction>();
	private Map<String, Map<String, Schema>> urlSchemas = new HashMap<String, Map<String, Schema>>();
	
//	private Map<String, Schema> schemas = new HashMap<String, Schema>();
	private List<String> schemasToScan;

	public DatabaseWalker() {
	}

	public void addConnection(DBType databaseType, String username, String password, String url) {
		DatabaseAbstraction dbAbstraction = DatabaseAbstractionFactory.getDatabaseAbstraction(databaseType, username, password, url);
		dbAbstractions.put(url, dbAbstraction);
		dbAbstraction.getAllSchemaNamesWithTables();
		
	}
	
	public void startWalking(String url) {
		DatabaseAbstraction dbAbstraction = dbAbstractions.get(url);
		if (dbAbstraction != null) {
			Map<String, Schema>
			getTableNames(dbAbstraction);
			getTableColumns(dbAbstraction);
			getTableConstrints(dbAbstraction);
			followTableConstrints(dbAbstraction);
		}
	}

	private void getTableNames(DatabaseAbstraction dbAbstraction) {
		schemas = dbAbstraction.getTableNames(schemasToScan);
	}

	private void getTableColumns(DatabaseAbstraction dbAbstraction) {
		try {
			dbAbstraction.getTableColumns(getSchemas());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getTableConstrints(DatabaseAbstraction dbAbstraction) {
			dbAbstraction.getTableConstrints(getSchemas());
	}

	private void followTableConstrints(DatabaseAbstraction dbAbstraction) {
		dbAbstraction.followTableConstrints(getSchemas());
	}

	public Map<String, Schema> getSchemas(String url) {
		return schemas;
	}

}
