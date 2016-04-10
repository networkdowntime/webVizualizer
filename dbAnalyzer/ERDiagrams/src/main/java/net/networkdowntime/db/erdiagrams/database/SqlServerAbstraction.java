package net.networkdowntime.db.erdiagrams.database;

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

import net.networkdowntime.db.erdiagrams.Column;
import net.networkdowntime.db.erdiagrams.Constraint;
import net.networkdowntime.db.erdiagrams.ConstraintType;
import net.networkdowntime.db.erdiagrams.Schema;
import net.networkdowntime.db.erdiagrams.Table;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;


public class SqlServerAbstraction implements DatabaseAbstraction {

	private QueryRunner run = new QueryRunner();
	private boolean debugOutput = false;
	private DataSource ds;

	public static void main(String[] args) {
		SqlServerAbstraction dba = new SqlServerAbstraction(true, "rganti", "TitleMax@", "jdbc:sqlserver://10.241.1.35;databaseName=Cashwise");
		dba.testConnection();

		Map<String, Schema> schemas = dba.getTableNames(dba.getAllSchemaNamesWithTables());
		
		dba.getTableColumns(schemas);
		dba.getTableConstrints(schemas);
		dba.followTableConstrints(schemas);
		
//		for (Schema schema : schemas.values()) {
//			System.out.println("Schema: " + schema.getName());
//			for (Table table : schema.getTables().values()) {
//				System.out.println("\t" + table.getName());
//			}
//		}
		
//		String query = "SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME FROM all_constraints WHERE CONSTRAINT_TYPE in ('P', 'U', 'R') and owner = '"
//				+ schema.getName() + "'";

		
		String test = "SELECT * from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where OBJECTPROPERTY(OBJECT_ID(constraint_name), 'IsPrimaryKey') = 1";
		
//		test = "select * from sys.tables";
//		test = "select * from INFORMATION_SCHEMA.TABLES";
//		test = "select CONSTRAINT_NAME,  from INFORMATION_SCHEMA.KEY_COLUMN_USAGE order by TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION";
//		test = "select * from INFORMATION_SCHEMA.KEY_COLUMN_USAGE order by TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION";
//		
//		test = "select OBJECT_ID(parent_object_id), name, 'column name', 'schema', referenced_object_id, 'referenced column'  from sys.foreign_keys";

//		test = "SELECT "
//				+ " K_Table = FK.TABLE_NAME,"
//				+ " Constraint_Name = C.CONSTRAINT_NAME,"
//				+ " FK_Column = CU.COLUMN_NAME,"
//				+ " PK.TABLE_SCHEMA,"
//				+ " PK_Table = PK.TABLE_NAME,"
//				+ " PK_Column = PT.COLUMN_NAME"
//				+ " FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS C"
//				+ " INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS FK ON C.CONSTRAINT_NAME = FK.CONSTRAINT_NAME"
//				+ " INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS PK ON C.UNIQUE_CONSTRAINT_NAME = PK.CONSTRAINT_NAME"
//				+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE CU ON C.CONSTRAINT_NAME = CU.CONSTRAINT_NAME"
//				+ " INNER JOIN ("
//				+ " SELECT i1.TABLE_NAME, i2.COLUMN_NAME"
//				+ " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS i1"
//				+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE i2 ON i1.CONSTRAINT_NAME = i2.CONSTRAINT_NAME"
//				+ " WHERE i1.CONSTRAINT_TYPE = 'PRIMARY KEY'"
//				+ " ) PT ON PT.TABLE_NAME = PK.TABLE_NAME";

		test = " SELECT i1.TABLE_SCHEMA, i1.TABLE_NAME, i1.CONSTRAINT_NAME, i2.COLUMN_NAME, null, null, null"
				+ " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS i1"
				+ " INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE i2 ON i1.CONSTRAINT_NAME = i2.CONSTRAINT_NAME"
				+ " WHERE i1.CONSTRAINT_TYPE = 'PRIMARY KEY'";
		
//		dba.testQuery(test);
		
	}
	
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
			System.out.println();

			while (rs.next()) {
				for (int i = 1; i <= columnCount; i++) {
					System.out.print(rs.getObject(i) + ",\t");
				}
				System.out.println();
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

	public SqlServerAbstraction(boolean debugOutput, String userName, String password, String url) {
		this.debugOutput = debugOutput;
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
		if (this.debugOutput)
			System.out.println("Begin getTableNames");
		long startTime = System.currentTimeMillis();

		Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();

		Connection conn = null;
		try {

			conn = ds.getConnection();

			for (String schemaName : schemasToScan) {
				Schema schema = new Schema(schemaName);
				schemas.put(schemaName, schema);

				String query = "SELECT t.name, '', i.rows FROM sys.tables AS t INNER JOIN  sys.sysindexes AS i ON t.object_id = i.id AND i.indid < 2 where SCHEMA_NAME(t.schema_id) = '" + schemaName + "'";

				List<Object[]> tableList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				if (this.debugOutput)
					System.out.println("Found tables:");
				for (Object[] tableInfo : tableList) {
					if (this.debugOutput)
						System.out.println("\t" + schema.getName() + "." + tableInfo[0] + ": " + tableInfo[1] + "; row count = " + tableInfo[2]);

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

		if (this.debugOutput)
			System.out.println("End getTableNames: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
		return schemas;
	}

	public void getTableColumns(Map<String, Schema> schemasToScan) {
		if (this.debugOutput)
			System.out.println("Begin getTableColumns");
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

				if (this.debugOutput)
					System.out.println(query);

				List<Object[]> columnList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				if (this.debugOutput)
					System.out.println("Found Columns For Tables in Schema " + schema.getName() + ":");
				for (Object[] columnInfo : columnList) {
					// if (this.debugOutput) System.out.println("\t" + columnInfo[0] + ": " + columnInfo[4]);
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

					if (this.debugOutput)
						System.out.println("\tFound Column " + col.getName() + " For Table " + tableName + " in Schema " + schema.getName() + ":");

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

					if (table != null)
						table.getColumns().put(col.getName(), col);
					else if (this.debugOutput)
						System.err.println("!!! Couldn't find table " + tableName);
				}

				if (this.debugOutput) {
					for (Table table : schema.getTables().values()) {
						System.out.println("Found Columns For Table " + table.getName() + ":");
						System.out.println("\t" + table.getColumns().keySet());
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

		if (this.debugOutput)
			System.out.println("End getTableColumns: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");

	}

	public void getTableConstrints(Map<String, Schema> schemasToScan) {
		if (this.debugOutput)
			System.out.println("Begin getTableConstraints");
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

				if (this.debugOutput)
					System.out.println(query);

				List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				if (this.debugOutput)
					System.out.println("Found Constraints For Schema " + schema.getName() + ":");
				for (Object[] constriantInfo : constraintList) {
					// if (this.debugOutput) System.out.println("\t" + constriantInfo[0] + ": " +
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
						if (this.debugOutput)
							System.out.println("\tFound " + con.getConstraintType() + " constraint " + con.getName() + " for table " + tableName);
						table.addConstraint(con.getName(), con);
					} else {
						if (this.debugOutput)
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

		if (this.debugOutput)
			System.out.println("End getTableConstraints: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
	}

	public void followTableConstrints(Map<String, Schema> schemasToScan) {
		if (this.debugOutput)
			System.out.println("Begin followTableConstraints");
		long startTime = System.currentTimeMillis();

		Connection conn = null;
		try {
			conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {
				if (this.debugOutput)
					System.out.println("Getting constraint references for " + schema.getName());

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

				if (this.debugOutput)
					System.out.println(query);

				List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				for (Object[] constriantInfo : constraintList) {
					String tableName = (String) constriantInfo[0];
					String constraintName = (String) constriantInfo[1];
					String columnName = (String) constriantInfo[2];
					String referencedSchema = (String) constriantInfo[3];
					String referencedTable = (String) constriantInfo[4];
					String referencedColumn = (String) constriantInfo[5];

					if (this.debugOutput)
						System.out.println("\t" + constraintName + " for table " + tableName + " column " + columnName + " referencing " + referencedSchema
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
									if (this.debugOutput)
										System.err.println("!!! Couldn't find referenced Table " + referencedTable);
								}
							} else {
								if (this.debugOutput)
									System.err.println("!!! Couldn't find referenced Schema " + referencedSchema);
							}
						} else {
							if (this.debugOutput)
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

		if (this.debugOutput)
			System.out.println("End followTableConstraints: " + ((System.currentTimeMillis() - startTime) / 1000 / 60.0) + " seconds");
	}

	// dot -Tpng -O tlx_schema.gv
	// dot -Tsvg -O tlx_schema.gv
}
