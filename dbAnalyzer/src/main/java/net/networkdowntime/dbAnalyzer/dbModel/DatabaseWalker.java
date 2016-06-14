package net.networkdowntime.dbAnalyzer.dbModel;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstraction;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory.DBType;

public class DatabaseWalker {

	private Map<String, DatabaseAbstraction> dbAbstractions = new HashMap<String, DatabaseAbstraction>();
	private Map<String, Map<String, Schema>> urlSchemas = new HashMap<String, Map<String, Schema>>();

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
		scanDatabase(url);
	}

	public boolean removeConnection(String url) {
		boolean exists = dbAbstractions.containsKey(url);
		dbAbstractions.remove(url);
		urlSchemas.remove(url);
		return exists;
	}

	public Set<String> getUrls() {
		return dbAbstractions.keySet();
	}

	public void scanDatabase(String url) {
		DatabaseAbstraction dbAbstraction = dbAbstractions.get(url);
		if (dbAbstraction != null) {
			Map<String, Schema> schemas = urlSchemas.get(url);
			schemas.putAll(getTableNames(dbAbstraction, schemas.keySet()));
			getTableColumns(schemas, dbAbstraction);
			getTableConstrints(schemas, dbAbstraction);
			followTableConstrints(schemas, dbAbstraction);
		}
	}

	public Map<String, Set<String>> getSchemas() {
		Map<String, Set<String>> urlSchemaMap = new LinkedHashMap<String, Set<String>>();
		for (String url : urlSchemas.keySet()) {
			Set<String> schemas = new LinkedHashSet<String>(urlSchemas.get(url).keySet());
			urlSchemaMap.put(url, schemas);
		}
		return urlSchemaMap;
	}

	public Set<String> getSchemas(String url) {
		Set<String> schemas = new LinkedHashSet<String>(urlSchemas.get(url).keySet());
		return schemas;
	}

	public Set<String> getTables(String url, String schemaName) {
		Set<String> tableSet = new HashSet<String>();
		if (urlSchemas.containsKey(url) && urlSchemas.get(url).containsKey(schemaName)) {
			tableSet.addAll(urlSchemas.get(url).get(schemaName).getTables().keySet());
		}
		return tableSet;
	}

	public Map<String, Set<String>> getTables(Map<String, Set<String>> urlSchemaMap) {
		Map<String, Set<String>> urlsTableMap = new LinkedHashMap<String, Set<String>>();
		for (String url : urlSchemaMap.keySet()) {
			if (urlSchemas.containsKey(url)) {
				Set<String> tables = urlsTableMap.get(url);
				if (tables == null) {
					tables = new LinkedHashSet<String>();
					urlsTableMap.put(url, tables);
				}
				for (String schemaName : urlSchemaMap.get(url)) {
					if (urlSchemas.get(url).containsKey(schemaName)) {
						Schema schema = urlSchemas.get(url).get(schemaName);
						for (Table table : schema.getTables().values()) {
							tables.add(schema.getName() + "." + table.getName());
						}
					}
				}
			}
		}
		return urlsTableMap;
	}

	public Schema getSchema(String url, String schemaName) {
		if (urlSchemas.containsKey(url)) {
			return urlSchemas.get(url).get(schemaName);
		}
		return null;
	}

	private Map<String, Schema> getTableNames(DatabaseAbstraction dbAbstraction, Collection<String> schemasToScan) {
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

}
