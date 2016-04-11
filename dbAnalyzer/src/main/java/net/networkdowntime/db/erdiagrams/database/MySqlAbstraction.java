package net.networkdowntime.db.erdiagrams.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.networkdowntime.db.erdiagrams.Column;
import net.networkdowntime.db.erdiagrams.Constraint;
import net.networkdowntime.db.erdiagrams.ConstraintType;
import net.networkdowntime.db.erdiagrams.Schema;
import net.networkdowntime.db.erdiagrams.Table;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class MySqlAbstraction implements DatabaseAbstraction {

	private QueryRunner run;
	private boolean debugOutput = false;
	private DataSource ds;

	@SuppressWarnings("unused")
	private MySqlAbstraction() {
	}

	public MySqlAbstraction(boolean debugOutput, String userName, String password, String url) {
		this.debugOutput = debugOutput;
		this.ds = createConnection(userName, password, url);
	}

	public DataSource getDataSource() {
		return ds;
	}

	private DataSource createConnection(String userName, String password, String url) {
		try {
			// Class.forName("com.mysql.jdbc.Driver").newInstance();
			// MysqlDataSource myDS = new MysqlDataSource();
			// myDS.setUser(userName);
			// myDS.setPassword(password);
			// myDS.setUrl(url);
			// return myDS;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String testConnection() {
		String query = "select 'test' from dual";

		Connection conn = null;
		try {
			if (ds != null) {
				conn = ds.getConnection();
				run.query(conn, query, new ArrayListHandler());
			} else {
				return "failure";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "success";
	}

	public List<String> getAllSchemaNamesWithTables() {
		List<String> schemaNames = new ArrayList<String>();

		try {

			Connection conn = ds.getConnection();

			String query = "select table_schema from information_schema.tables group by table_schema order by table_schema";

			List<Object[]> schemaList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

			for (Object[] arr : schemaList) {
				schemaNames.add((String) arr[0]);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return schemaNames;
	}

	public Map<String, Schema> getTableNames(List<String> schemasToScan) {
		Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();

		try {

			Connection conn = ds.getConnection();

			for (String schemaName : schemasToScan) {
				Schema schema = new Schema(schemaName);
				schemas.put(schemaName, schema);

				String query = "select table_name, table_comment from information_schema.tables where table_schema = '" + schemaName + "'";

				List<Object[]> tableList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				if (this.debugOutput)
					System.out.println("Found tables:");
				for (Object[] tableInfo : tableList) {
					if (this.debugOutput)
						System.out.println("\t" + schema.getName() + "." + tableInfo[0] + ": " + tableInfo[1]);
					schema.addTable((String) tableInfo[0], (String) tableInfo[1]);
				}

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return schemas;
	}

	public void getTableColumns(Map<String, Schema> schemasToScan) throws SQLException {
		Connection conn = ds.getConnection();

		for (Schema schema : schemasToScan.values()) {

			for (String tableName : schema.getTables().keySet()) {
				Table table = schema.getTables().get(tableName);

				String query = "select column_name, ordinal_position, column_default, is_nullable, data_type, column_type, extra, column_comment, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE from information_schema.columns where table_schema = '"
						+ schema.getName() + "' and table_name = '" + table.getName() + "'";

				List<Object[]> columnList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());
				LinkedHashMap<String, Column> columns = new LinkedHashMap<String, Column>();

				if (this.debugOutput)
					System.out.println("Found Columns For Table " + schema.getName() + "." + table.getName() + ":");
				for (Object[] columnInfo : columnList) {
					// if (this.debugOutput) System.out.println("\t" + columnInfo[0] + ": " + columnInfo[4]);
					Column col = new Column((String) columnInfo[0]);
					col.setOrdinalPosition(((Long) columnInfo[1]).intValue());
					col.setColumnDefault((String) columnInfo[2]);
					col.setNullable(("YES".equals((String) columnInfo[3]) ? true : false));
					col.setDataType((String) columnInfo[4]);
					col.setColumnType((String) columnInfo[5]);
					col.setExtra((String) columnInfo[6]);
					col.setComment((String) columnInfo[7]);
					col.setLength((BigDecimal) columnInfo[8]);
					col.setPrecision((BigDecimal) columnInfo[9]);
					col.setScale((BigDecimal) columnInfo[10]);
					columns.put(col.getName(), col);
				}

				table.setColumns(columns);

				if (this.debugOutput)
					System.out.println("\t" + table.getColumns().keySet());
			}
		}
	}

	public void getTableConstrints(Map<String, Schema> schemasToScan) {
		try {
			Connection conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {

				for (String tableName : schema.getTables().keySet()) {
					Table table = schema.getTables().get(tableName);

					String query = "select " + "constraint_name, " + "constraint_type " + "from information_schema.table_constraints "
							+ "where table_schema = '" + schema.getName() + "' and table_name = '" + tableName + "'";

					List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

					if (this.debugOutput)
						System.out.println("Found Constraints For Table " + schema.getName() + "." + tableName + ":");
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
						table.addConstraint(con.getName(), con);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void followTableConstrints(Map<String, Schema> schemasToScan) {
		//		try {
		//			for (String schemaName : schemasToScan) {
		//				Schema schema = schemas.get(schemaName);
		//
		//				for (String tableName : schema.tables.keySet()) {
		//					Table table = schema.tables.get(tableName);
		//
		//					if (this.debugOutput)
		//						System.out.println("Found Constraints For Table " + schema.name + "." + tableName + ":");
		//					for (String constriantName : table.constraints.keySet()) {
		//						Constraint con = table.constraints.get(constriantName);
		//
		//						String query = "select " + "column_name, " + "referenced_table_schema, " + "referenced_table_name, " + "referenced_column_name "
		//								+ "from information_schema.key_column_usage " + "where table_schema = '" + schemaName + "' " + "and table_name = '" + tableName
		//								+ "' " + "and constraint_name = '" + con.name + "'";
		//
		//						// if (this.debugOutput) System.out.println(query);
		//						List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());
		//
		//						for (Object[] constriantInfo : constraintList) {
		//							if (this.debugOutput)
		//								System.out.println("\t" + con.name + " for column " + constriantInfo[0]);
		//							Column col = table.columns.get((String) constriantInfo[0]);
		//							con.columns.add(col);
		//							if (constriantInfo[1] != null) {
		//								Schema refSchema = schemas.get((String) constriantInfo[1]);
		//								con.refSchema = refSchema;
		//								Table refTable = refSchema.tables.get((String) constriantInfo[2]);
		//								con.refTable = refTable;
		//								Column refColumn = refTable.columns.get((String) constriantInfo[3]);
		//								con.refColumn.add(refColumn);
		//							}
		//						}
		//					}
		//				}
		//			}
		//		} catch (SQLException e) {
		//			e.printStackTrace();
		//		}
	}

}
