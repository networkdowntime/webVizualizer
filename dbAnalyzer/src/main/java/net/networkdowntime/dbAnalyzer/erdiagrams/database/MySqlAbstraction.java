package net.networkdowntime.dbAnalyzer.erdiagrams.database;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.networkdowntime.dbAnalyzer.erdiagrams.Column;
import net.networkdowntime.dbAnalyzer.erdiagrams.Constraint;
import net.networkdowntime.dbAnalyzer.erdiagrams.ConstraintType;
import net.networkdowntime.dbAnalyzer.erdiagrams.Schema;
import net.networkdowntime.dbAnalyzer.erdiagrams.Table;

public class MySqlAbstraction implements DatabaseAbstraction {

	private QueryRunner run = new QueryRunner();
	private boolean debugOutput = false;
	private DataSource ds;

	@SuppressWarnings("unused")
	private MySqlAbstraction() {
	}

	public MySqlAbstraction(boolean debugOutput, String userName, String password, String url) {
		this.debugOutput = debugOutput;
		this.ds = createConnection(userName, password, url);
		if (debugOutput) {
			System.out.println("dataSource == null: " + (this.ds == null));
			System.out.println(testConnection());
		}
	}

	public DataSource getDataSource() {
		return ds;
	}

	private DataSource createConnection(String userName, String password, String url) {
		try {
			if (debugOutput)
				System.out.println("Creating MySql data source");
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			MysqlDataSource myDS = new MysqlDataSource();
			myDS.setUser(userName);
			myDS.setPassword(password);
			myDS.setURL(url);
			return myDS;

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
		String query = "select table_schema from information_schema.tables group by table_schema order by table_schema";

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

				String query = "select table_name, table_comment, table_rows from information_schema.tables where table_schema = '" + schemaName + "'";

				List<Object[]> tableList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				if (this.debugOutput)
					System.out.println("Found tables:");
				for (Object[] tableInfo : tableList) {
					if (this.debugOutput)
						System.out.println("\t" + schema.getName() + "." + tableInfo[0] + ": " + tableInfo[1]);

					BigDecimal numberOfRows = null;
					if (tableInfo[2] != null) {
						numberOfRows = new BigDecimal((BigInteger) tableInfo[2]);
					} else {
						numberOfRows = run.query(conn, "SELECT count(*) FROM " + schemaName + "." + ((String) tableInfo[0]),
								new ResultSetHandler<BigDecimal>() {

									public BigDecimal handle(ResultSet rs) throws SQLException {
										if (rs.next()) {
											return rs.getBigDecimal(1);
										}
										return new BigDecimal(0);
									}
								});
					}
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
			System.out.println("End getTableNames: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
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

				for (String tableName : schema.getTables().keySet()) {
					Table table = schema.getTables().get(tableName);

					String query = "select column_name, ordinal_position, column_default, is_nullable, data_type, column_type, extra, column_comment, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE from information_schema.columns where table_schema = '"
							+ schema.getName() + "' and table_name = '" + table.getName() + "'";

					if (this.debugOutput)
						System.out.println(query);

					List<Object[]> columnList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());
					LinkedHashMap<String, Column> columns = new LinkedHashMap<String, Column>();

					if (this.debugOutput)
						System.out.println("Found Columns For Table " + schema.getName() + "." + table.getName() + ":");
					for (Object[] columnInfo : columnList) {
						// if (this.debugOutput) System.out.println("\t" + columnInfo[0] + ": " + columnInfo[4]);
						Column col = new Column((String) columnInfo[0]);
						col.setOrdinalPosition(((BigInteger) columnInfo[1]).intValue());
						col.setColumnDefault((String) columnInfo[2]);
						col.setNullable(("YES".equals((String) columnInfo[3]) ? true : false));
						col.setDataType((String) columnInfo[4]);
						col.setColumnType((String) columnInfo[5]);
						col.setExtra((String) columnInfo[6]);
						col.setComment((String) columnInfo[7]);
						if (columnInfo[8] != null) {
							col.setLength(new BigDecimal((BigInteger) columnInfo[8]));
						} else {
							col.setLength(null);
						}
						if (columnInfo[9] != null) {
							col.setPrecision(new BigDecimal((BigInteger) columnInfo[9]));
						} else {
							col.setPrecision(null);
						}
						if (columnInfo[10] != null) {
							col.setScale(new BigDecimal((BigInteger) columnInfo[10]));
						} else {
							col.setScale(null);
						}
						columns.put(col.getName(), col);
					}

					table.setColumns(columns);

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
			System.out.println("End getTableColumns: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");

	}

	public void getTableConstrints(Map<String, Schema> schemasToScan) {
		if (this.debugOutput)
			System.out.println("Begin getTableConstraints");
		long startTime = System.currentTimeMillis();

		Connection conn = null;
		try {
			conn = ds.getConnection();

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
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (this.debugOutput)
			System.out.println("End getTableConstraints: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
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

				for (Table table : schema.getTables().values()) {

					if (this.debugOutput)
						System.out.println("Found Constraints For Table " + schema.getName() + "." + table.getName() + ":");
					for (Constraint con : table.getConstraints().values()) {

						String query = "select " + "column_name, " + "referenced_table_schema, " + "referenced_table_name, " + "referenced_column_name "
								+ "from information_schema.key_column_usage " + "where table_schema = '" + schema.getName() + "' " + "and table_name = '" + table.getName()
								+ "' " + "and constraint_name = '" + con.getName() + "'";

						// if (this.debugOutput) System.out.println(query);
						List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

						for (Object[] constriantInfo : constraintList) {
							if (this.debugOutput)
								System.out.println("\t" + con.getName() + " for column " + constriantInfo[0]);
							Column col = table.getColumns().get((String) constriantInfo[0]);
							con.getColumns().add(col);
							if (constriantInfo[1] != null) {
								Schema refSchema = schemasToScan.get((String) constriantInfo[1]);
								con.setRefSchema(refSchema);
								Table refTable = refSchema.getTables().get((String) constriantInfo[2]);
								con.setRefTable(refTable);
								Column refColumn = refTable.getColumns().get((String) constriantInfo[3]);
								con.getRefColumn().add(refColumn);
							}
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
			System.out.println("End followTableConstraints: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
	}

}
