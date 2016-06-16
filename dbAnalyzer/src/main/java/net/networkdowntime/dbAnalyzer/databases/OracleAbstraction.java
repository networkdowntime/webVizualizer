package net.networkdowntime.dbAnalyzer.databases;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

import net.networkdowntime.dbAnalyzer.dbModel.Column;
import net.networkdowntime.dbAnalyzer.dbModel.Constraint;
import net.networkdowntime.dbAnalyzer.dbModel.ConstraintType;
import net.networkdowntime.dbAnalyzer.dbModel.Schema;
import net.networkdowntime.dbAnalyzer.dbModel.Table;
import oracle.jdbc.pool.OracleDataSource;

public class OracleAbstraction implements DatabaseAbstraction {
	private static final Logger LOGGER = LogManager.getLogger(OracleAbstraction.class.getName());

	private QueryRunner run = new QueryRunner();
	private DataSource ds;

	@SuppressWarnings("unused")
	private OracleAbstraction() {
	}

	public OracleAbstraction(String userName, String password, String url) {
		this.ds = createConnection(userName, password, url);
	}

	private DataSource createConnection(String userName, String password, String url) {
		try {
			Class.forName("oracle.jdbc.OracleDriver").newInstance();

			OracleDataSource myDS = new OracleDataSource();

			// set connection properties
			Properties info = new java.util.Properties();
			info.put("defaultRowPrefetch", "1000");

			myDS.setConnectionProperties(info);
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

			conn = ds.getConnection();
			run.query(conn, query, new ArrayListHandler());

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return "success";
	}

	public List<String> getAllSchemaNamesWithTables() {
		List<String> schemaNames = new ArrayList<String>();
		String query = "select owner from ALL_OBJECTS where object_type = 'TABLE' group by owner order by owner";

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

	@Override
	public Map<String, Schema> getTableNames(Collection<String> schemasToScan) {
		LOGGER.debug("Begin getTableNames");
		long startTime = System.currentTimeMillis();

		Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();

		Connection conn = null;
		try {

			conn = ds.getConnection();

			for (String schemaName : schemasToScan) {
				Schema schema = new Schema(schemaName);
				schemas.put(schemaName, schema);

				String query = "SELECT table_name, '', NUM_ROWS FROM all_tables where owner = '" + schemaName + "' order by table_name";

				List<Object[]> tableList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found tables:");
				for (Object[] tableInfo : tableList) {
					LOGGER.debug("\t" + schema.getName() + "." + tableInfo[0] + ": " + tableInfo[1]);

					BigDecimal numberOfRows = (BigDecimal) tableInfo[2];
					if (numberOfRows == null) {
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

		LOGGER.debug("End getTableNames: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
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
						+ ", COLUMN_ID"
						+ ", ''" // , DATA_DEFAULT" // Ignoring the default values because this is
						// of type LONG and it forces oracle to use a fetch size of 1! Verry slowwww!!!
						+ ", NULLABLE"
						+ ", DATA_TYPE"
						+ ", ''"
						+ ", ''"
						+ ", 'comment'"
						+ ", CHAR_LENGTH"
						+ ", DATA_PRECISION"
						+ ", DATA_SCALE"
						+ ", TABLE_NAME"
						+ " from all_tab_columns"
						+ " where OWNER = '" + schema.getName() + "'"
						+ " order by TABLE_NAME, column_id";

				LOGGER.debug(query);

				List<Object[]> columnList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found Columns For Tables in Schema " + schema.getName() + ":");
				for (Object[] columnInfo : columnList) {
					// if (this.debugOutput) LOGGER.debug("\t" + columnInfo[0] + ": " + columnInfo[4]);
					Column col = new Column((String) columnInfo[0]);
					col.setOrdinalPosition(((BigDecimal) columnInfo[1]).intValue());
					col.setColumnDefault((String) columnInfo[2]);
					col.setNullable(("Y".equals((String) columnInfo[3]) ? true : false));
					col.setDataType((String) columnInfo[4]);
					col.setExtra((String) columnInfo[6]);
					col.setComment((String) columnInfo[7]);
					col.setLength((BigDecimal) columnInfo[8]);

					col.setPrecision((BigDecimal) columnInfo[9]);
					col.setScale((BigDecimal) columnInfo[10]);
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
						LOGGER.error("!!! Couldn't find table " + tableName);
					}
				}

				for (Table table : schema.getTables().values()) {
					LOGGER.debug("Found Columns For Table " + table.getName() + ":");
					LOGGER.debug("\t" + table.getColumns().keySet());
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

		LOGGER.debug("End getTableColumns: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");

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

				String query = "SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME FROM all_constraints WHERE CONSTRAINT_TYPE in ('P', 'U', 'R') and owner = '"
						+ schema.getName() + "'";

				LOGGER.debug(query);

				List<Object[]> constraintList = (List<Object[]>) run.query(conn, query, new ArrayListHandler());

				LOGGER.debug("Found Constraints For Schema " + schema.getName() + ":");
				for (Object[] constriantInfo : constraintList) {
					// if (this.debugOutput) LOGGER.debug("\t" + constriantInfo[0] + ": " +
					// constriantInfo[1]);
					Constraint con = new Constraint((String) constriantInfo[0]);
					if ("P".equals(constriantInfo[1])) {
						con.setConstraintType(ConstraintType.PRIMARY_KEY);
					} else if ("R".equals(constriantInfo[1])) {
						con.setConstraintType(ConstraintType.FOREIGN_KEY);
					} else if ("U".equals(constriantInfo[1])) {
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

		LOGGER.debug("End getTableConstraints: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
	}

	public void followTableConstrints(Map<String, Schema> schemasToScan) {
		LOGGER.debug("Begin followTableConstraints");
		long startTime = System.currentTimeMillis();

		Connection conn = null;
		try {
			conn = ds.getConnection();

			for (Schema schema : schemasToScan.values()) {
				LOGGER.debug("Getting constraint references for " + schema.getName());

				String query = "SELECT distinct "
						+ " ac.table_name" // 0
						+ ", ac.constraint_name" // 1
						+ ", acc1.column_name" // 2
						+ ", ac.r_owner" // 3
						+ ", acc2.table_name" // 4
						+ ", acc2.column_name" // 5
						+ " FROM all_cons_columns acc1, all_constraints ac left join all_cons_columns acc2 on ac.r_constraint_name=acc2.constraint_name"
						+ " WHERE"
						+ " ac.constraint_name=acc1.constraint_name"
						+ " and ac.owner = '" + schema.getName() + "'"
						+ " and ac.constraint_type in ('P', 'R')";

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

		LOGGER.debug("End followTableConstraints: " + ((System.currentTimeMillis() - startTime) / 60.0) + " seconds");
	}

	// dot -Tpng -O tlx_schema.gv
	// dot -Tsvg -O tlx_schema.gv
}
