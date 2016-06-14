package net.networkdowntime.dbAnalyzer.databases;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.networkdowntime.dbAnalyzer.dbModel.Schema;


public interface DatabaseAbstraction {

	public List<String> getAllSchemaNamesWithTables();
	
	public Map<String, Schema> getTableNames(Collection<String> schemasToScan);

	public void getTableColumns(Map<String, Schema> schemasToScan) throws SQLException;

	/**
	 * Processes the Schema and their Tables and adds the constraints found in the Database.
	 * 
	 * @param schemasToScan
	 */
	public void getTableConstrints(Map<String, Schema> schemasToScan);

	public void followTableConstrints(Map<String, Schema> schemasToScan);

	/**
	 * Tests if the Database connection is working
	 * 
	 * @return
	 */
	public String testConnection();
}
