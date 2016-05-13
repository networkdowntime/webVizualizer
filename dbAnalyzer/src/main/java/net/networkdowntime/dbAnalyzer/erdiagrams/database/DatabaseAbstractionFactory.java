package net.networkdowntime.dbAnalyzer.erdiagrams.database;

public class DatabaseAbstractionFactory {

	public enum DBType {
		MySql, Oracle, SqlServer
	}

	public static DatabaseAbstraction getDatabaseAbstraction(boolean debugOutput, DBType databaseType, String username, String password, String url) {
		DatabaseAbstraction abstraction = null;
		
		if (debugOutput) System.out.println("Attempting to create a DB abstraction of type: " + databaseType.name());

		switch (databaseType) {
		case MySql:
			abstraction = (DatabaseAbstraction) new MySqlAbstraction(debugOutput, username, password, url);
			break;
		case Oracle:
			abstraction = (DatabaseAbstraction) new OracleAbstraction(debugOutput, username, password, url);
			break;
		case SqlServer:
			abstraction = (DatabaseAbstraction) new SqlServerAbstraction(debugOutput, username, password, url);
			break;
		}
		
		return abstraction;
	}
}
