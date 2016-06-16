package net.networkdowntime.dbAnalyzer.databases;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseAbstractionFactory {
	private static final Logger LOGGER = LogManager.getLogger(DatabaseAbstractionFactory.class.getName());

	public enum DBType {
		MySql, Oracle, SqlServer
	}

	public static DatabaseAbstraction getDatabaseAbstraction(DBType databaseType, String username, String password, String url) {
		DatabaseAbstraction abstraction = null;

		LOGGER.debug("Attempting to create a DB abstraction of type: " + databaseType.name());

		switch (databaseType) {
		case MySql:
			abstraction = (DatabaseAbstraction) new MySqlAbstraction(username, password, url);
			break;
		case Oracle:
			abstraction = (DatabaseAbstraction) new OracleAbstraction(username, password, url);
			break;
		case SqlServer:
			abstraction = (DatabaseAbstraction) new SqlServerAbstraction(username, password, url);
			break;
		}

		return abstraction;
	}
}
