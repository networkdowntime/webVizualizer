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
	
	private List<String> schemasToScan;

	public DatabaseWalker() {
	}

	public void addConnection(DBType databaseType, String username, String password, String url) {
		DatabaseAbstraction dbAbstraction = DatabaseAbstractionFactory.getDatabaseAbstraction(databaseType, username, password, url);
		dbAbstractions.put(url, dbAbstraction);
		Map<String, Schema> schemas = new HashMap<String, Schema>();
		urlSchemas.put(url, schemas);
		for (String schemaName : dbAbstraction.getAllSchemaNamesWithTables()) {
			schemas.put(schemaName, new Schema(schemaName));
		}
		
	}
	
	public boolean removeConnection(String url) {
		boolean exists = dbAbstractions.containsKey(url);
		dbAbstractions.remove(url);
		urlSchemas.remove(url);
		return exists;
	}
	
	public void scanDatabase(String url) {
		DatabaseAbstraction dbAbstraction = dbAbstractions.get(url);
		if (dbAbstraction != null) {
			Map<String, Schema> schemas = urlSchemas.get(url);
			schemas.putAll(getTableNames(dbAbstraction));
			getTableColumns(schemas, dbAbstraction);
			getTableConstrints(schemas, dbAbstraction);
			followTableConstrints(schemas, dbAbstraction);
		}
	}

	private Map<String, Schema> getTableNames(DatabaseAbstraction dbAbstraction) {
		return dbAbstraction.getTableNames(schemasToScan);
	}

	private void getTableColumns(Map<String, Schema> schemas, DatabaseAbstraction dbAbstraction) {
		try {
			dbAbstraction.getTableColumns(schemas);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getTableConstrints(Map<String, Schema> schemas, DatabaseAbstraction dbAbstraction) {
			dbAbstraction.getTableConstrints(schemas);
	}

	private void followTableConstrints(Map<String, Schema> schemas, DatabaseAbstraction dbAbstraction) {
		dbAbstraction.followTableConstrints(schemas);
	}

	public Map<String, Schema> getSchemas(String url) {
		return urlSchemas.get(url);
	}

}
