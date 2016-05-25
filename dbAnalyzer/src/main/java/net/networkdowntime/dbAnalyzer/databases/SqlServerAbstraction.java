package net.networkdowntime.dbAnalyzer.databases;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import net.networkdowntime.dbAnalyzer.dbModel.Column;
import net.networkdowntime.dbAnalyzer.dbModel.Constraint;
import net.networkdowntime.dbAnalyzer.dbModel.ConstraintType;
import net.networkdowntime.dbAnalyzer.dbModel.Schema;
import net.networkdowntime.dbAnalyzer.dbModel.Table;

public class SqlServerAbstraction implements DatabaseAbstraction {
	private static final Logger LOGGER = LogManager.getLogger("javaModel");

	private QueryRunner run = new QueryRunner();
	private DataSource ds;

	public void testQuery(String query) {
		Connection conn = null;
		try {

			conn = ds.getConnection();

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			for (int i = 1; i <= columnCount; i++) {
				System.out.print(rsmd.getColumnName(i) + ",\t");
			}
			LOGGER.debug("");

			while (rs.next()) {
				for (int i = 1; i <= columnCount; i++) {
					System.out.print(rs.getObject(i) + ",\t");
				}
				LOGGER.debug("");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unused")
	private SqlServerAbstraction() {
	}

	public SqlServerAbstraction(String userName, String password, String url) {
		this.ds = createConnection(userName, password, url);
	}

	private DataSource createConnection(String userName, String password, String url) {
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
			SQLServerDataSource myDS = new SQLServerDataSource();

			// set connection properties
			Properties info = new java.util.Properties();
			info.put("defaultRowPrefetch", "1000");

			//			myDS.setConnectionProperties(info);
			myDS.setUser(userName);
			myDS.setPassword(password);
			myDS.setURL(url);
			//		    myDS.setDatabaseName("Cashwise");

			//		    dataSource.setUser("aUser");
			//		    dataSource.setPassword("password");
			//		    dataSource.setServerName("hostname");

			return myDS;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String testConnection() {
		String retval = "success";
		String query = "select 1";

		Connection conn = null;
		try {

			conn = ds.getConnection();

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			rs.next();
			rs.getObject(1);

		} catch (SQLException e) {
			retval = "failure";
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				retval = "failure";
				e.printStackTrace();
			}
		}

		return retval;
	}

	public List<String> getAllSchemaNamesWithTables() {
		List<String> schemaNames = new ArrayList<String>();
		String query = "SELECT name FROM sys.schemas order by name";

		Connection conn = null;
		try {

			conn = ds.getConnection();
			List<Object[]> schemaList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

			for (Object[] arr : schemaList) {
				schemaNames.add((String) arr[0]);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return schemaNames;
	}

	public Map<String, Schema> getTableNames(List<String> schemasToScan) {
		LOGGER.debug("Begin getTableNames");
		long startTime = System.currentTimeMillis();

		Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();

		Connection conn = null;
		try {

			conn = ds.getConnection();

			for (String schemaName : schemasToScan) {
				Schema schema = new Schema(schemaName);
				schemas.put(schemaName, schema);

				String query = "SELECT t.name, '', i.rows FROM sys.tables AS t INNER JOIN  sys.sysindexes AS i ON t.object_id = i.id AND i.indid < 2 where SCHEMA_NAME(t.schema_id) = '" + schemaName
						+ "'";

				List<Object[]> tableList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found tables:");
				for (Object[] tableInfo : tableList) {
					LOGGER.debug("\t" + schema.getName() + "." + tableInfo[0] + ": " + tableInfo[1] + "; row count = " + tableInfo[2]);

					BigDecimal numberOfRows = new BigDecimal((Integer) tableInfo[2]);
					schema.addTable((String) tableInfo[0], (String) tableInfo[1], numberOfRows);
				}

			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("End getTableNames: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
		return schemas;
	}

	public void getTableColumns(Map<String, Schema> schemasToScan) {
		LOGGER.debug("Begin getTableColumns");
		long startTime = System.currentTimeMillis();

		Connection conn = null;

		try {

			conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {

				String query = "select"
						+ " COLUMN_NAME"
						+ ", ORDINAL_POSITION"
						+ ", COLUMN_DEFAULT"
						+ ", IS_NULLABLE"
						+ ", DATA_TYPE"
						+ ", ''"
						+ ", ''"
						+ ", 'comment'"
						+ ", CHARACTER_MAXIMUM_LENGTH"
						+ ", NUMERIC_PRECISION"
						+ ", NUMERIC_SCALE"
						+ ", TABLE_NAME "
						+ " FROM INFORMATION_SCHEMA.COLUMNS"
						+ " WHERE TABLE_SCHEMA = '" + schema.getName() + "'"
						+ " order by TABLE_NAME, ORDINAL_POSITION";

				LOGGER.debug(query);

				List<Object[]> columnList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found Columns For Tables in Schema " + schema.getName() + ":");
				for (Object[] columnInfo : columnList) {
					// if (this.debugOutput) LOGGER.debug("\t" + columnInfo[0] + ": " + columnInfo[4]);
					Column col = new Column((String) columnInfo[0]);
					col.setOrdinalPosition(((Integer) columnInfo[1]).intValue());
					col.setColumnDefault((String) columnInfo[2]);
					col.setNullable(("Y".equals((String) columnInfo[3]) ? true : false));
					col.setDataType((String) columnInfo[4]);
					col.setExtra((String) columnInfo[6]);
					col.setComment((String) columnInfo[7]);
					if (columnInfo[8] != null)
						col.setLength(new BigDecimal((Integer) columnInfo[8]));

					if (columnInfo[9] != null)
						col.setPrecision(new BigDecimal((Short) columnInfo[9]));

					if (columnInfo[10] != null)
						col.setScale(new BigDecimal((Integer) columnInfo[10]));

					String tableName = (String) columnInfo[11];

					LOGGER.debug("\tFound Column " + col.getName() + " For Table " + tableName + " in Schema " + schema.getName() + ":");

					if (col.getPrecision() != null) {
						StringBuffer columnType = new StringBuffer();
						columnType.append(((String) columnInfo[4]) + "(" + col.getPrecision());
						if (col.getScale() != null) {
							columnType.append("," + col.getScale());

						}
						columnType.append(")");
						col.setColumnType(columnType.toString());

					} else if (col.getLength() != null) {
						col.setColumnType(col.getDataType() + "(" + col.getLength() + ")");
					}

					Table table = schema.getTables().get(tableName);

					if (table != null) {
						table.getColumns().put(col.getName(), col);
					} else {
						System.err.println("!!! Couldn't find table " + tableName);
					}
				}

				if (LOGGER.isDebugEnabled()) {
					for (Table table : schema.getTables().values()) {
						LOGGER.debug("Found Columns For Table " + table.getName() + ":");
						LOGGER.debug("\t" + table.getColumns().keySet());
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("End getTableColumns: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");

	}

	public void getTableConstrints(Map<String, Schema> schemasToScan) {
		LOGGER.debug("Begin getTableConstraints");
		long startTime = System.currentTimeMillis();

		/*
		 * constraint_type
		 * C (check constraint on a table)
		 * P (primary key)
		 * U (unique key)
		 * R (referential integrity)
		 * V (with check option, on a view)
		 * O (with read only, on a view)
		 */
		Connection conn = null;
		try {
			conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {

				String query = "select CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME from INFORMATION_SCHEMA.TABLE_CONSTRAINTS where constraint_type in ('PRIMARY KEY', 'FOREIGN KEY', 'UNIQUE') and TABLE_SCHEMA = '"
						+ schema.getName() + "'";

				LOGGER.debug(query);

				List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found Constraints For Schema " + schema.getName() + ":");
				for (Object[] constriantInfo : constraintList) {
					// if (this.debugOutput) LOGGER.debug("\t" + constriantInfo[0] + ": " +
					// constriantInfo[1]);
					Constraint con = new Constraint((String) constriantInfo[0]);
					if ("PRIMARY KEY".equals(constriantInfo[1])) {
						con.setConstraintType(ConstraintType.PRIMARY_KEY);
					} else if ("FOREIGN KEY".equals(constriantInfo[1])) {
						con.setConstraintType(ConstraintType.FOREIGN_KEY);
					} else if ("UNIQUE".equals(constriantInfo[1])) {
						con.setConstraintType(ConstraintType.UNIQUE);
					}
					String tableName = (String) constriantInfo[2];

					Table table = schema.getTables().get(tableName);
					if (table != null) {
						LOGGER.debug("\tFound " + con.getConstraintType() + " constraint " + con.getName() + " for table " + tableName);
						table.addConstraint(con.getName(), con);
					} else {
						System.err.println("!!! Couldn't find table " + tableName);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("End getTableConstraints: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
	}

	public void followTableConstrints(Map<String, Schema> schemasToScan) {
		LOGGER.debug("Begin followTableConstraints");
		long startTime = System.currentTimeMillis();

		Connection conn = null;
		try {
			conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {
				LOGGER.debug("Getting constraint references for " + schema.getName());

				String query = "SELECT "
						+ " K_Table = FK.TABLE_NAME," // 0
						+ " Constraint_Name = C.CONSTRAINT_NAME," // 1
						+ " FK_Column = CU.COLUMN_NAME," // 2
						+ " PK.TABLE_SCHEMA," // 3
						+ " PK_Table = PK.TABLE_NAME," // 4
						+ " PK_Column = PT.COLUMN_NAME"  // 5
						+ " FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS C"
						+ " INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS FK ON C.CONSTRAINT_NAME = FK.CONSTRAINT_NAME"
						+ " INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS PK ON C.UNIQUE_CONSTRAINT_NAME = PK.CONSTRAINT_NAME"
						+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE CU ON C.CONSTRAINT_NAME = CU.CONSTRAINT_NAME"
						+ " INNER JOIN ("
						+ " SELECT i1.TABLE_NAME, i2.COLUMN_NAME"
						+ " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS i1"
						+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE i2 ON i1.CONSTRAINT_NAME = i2.CONSTRAINT_NAME"
						+ " WHERE i1.CONSTRAINT_TYPE = 'PRIMARY KEY'"
						+ " ) PT ON PT.TABLE_NAME = PK.TABLE_NAME where PK.TABLE_SCHEMA = '" + schema.getName() + "'";
				//						+ " union SELECT i1.TABLE_NAME, i1.CONSTRAINT_NAME, i2.COLUMN_NAME, null, null, null"
				//						+ " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS i1"
				//						+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE i2 ON i1.CONSTRAINT_NAME = i2.CONSTRAINT_NAME"
				//						+ " WHERE i1.CONSTRAINT_TYPE = 'PRIMARY KEY' AND i1.TABLE_SCHEMA = '" + schema.getName() + "'";

				LOGGER.debug(query);

				List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				for (Object[] constriantInfo : constraintList) {
					String tableName = (String) constriantInfo[0];
					String constraintName = (String) constriantInfo[1];
					String columnName = (String) constriantInfo[2];
					String referencedSchema = (String) constriantInfo[3];
					String referencedTable = (String) constriantInfo[4];
					String referencedColumn = (String) constriantInfo[5];

					LOGGER.debug("\t" + constraintName + " for table " + tableName + " column " + columnName + " referencing " + referencedSchema
							+ "." + referencedTable + "." + referencedColumn);

					Table table = schema.getTables().get(tableName);
					if (table != null) {
						Constraint con = table.getConstraints().get(constraintName);

						Column col = table.getColumns().get(columnName);
						con.getColumns().add(col);
						if (con.getConstraintType() == ConstraintType.FOREIGN_KEY && referencedSchema != null) {
							Schema refSchema = schemasToScan.get(referencedSchema);
							if (refSchema != null) {
								con.setRefSchema(refSchema);
								Table refTable = refSchema.getTables().get(referencedTable);
								if (refTable != null) {
									con.setRefTable(refTable);
									Column refColumn = refTable.getColumns().get(referencedColumn);
									con.getRefColumn().add(refColumn);
								} else {
									System.err.println("!!! Couldn't find referenced Table " + referencedTable);
								}
							} else {
								System.err.println("!!! Couldn't find referenced Schema " + referencedSchema);
							}
						} else {
							System.err.println("!!! Referenced Schema string is null: " + referencedSchema);
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		LOGGER.debug("End followTableConstraints: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
	}

	// dot -Tpng -O tlx_schema.gv
	// dot -Tsvg -O tlx_schema.gv
}
