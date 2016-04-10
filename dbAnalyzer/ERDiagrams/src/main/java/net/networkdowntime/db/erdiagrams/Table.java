package net.networkdowntime.db.erdiagrams;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;


public @Data class Table {

	/**
	 * The name of the table
	 */
	private String name;
	
	/**
	 * The comments for the table
	 */
	private String comment;
	
	/**
	 * The number of rows in the database for this table
	 */
	private BigDecimal numberOfRows;
	
	/**
	 * A map of the columns in this table
	 */
	private Map<String, Column> columns = new LinkedHashMap<String, Column>();
	
	/**
	 * A map of the constraints defined for this table
	 */
	private Map<String, Constraint> constraints = new LinkedHashMap<String, Constraint>();

	public Table(String name, String comment) {
		this.name = name;
		this.comment = comment;
	}

	public Table(String name, String comment, BigDecimal numberOfRows) {
		this.name = name;
		this.comment = comment;
		this.numberOfRows = numberOfRows;
	}

	public void addColumn(String name, Column col) {
		columns.put(name, col);
	}

	public void addConstraint(String name, Constraint con) {
		constraints.put(name, con);
	}

	public boolean isColumnPK(Column col) {
		boolean pk = false;
		for (Constraint con : constraints.values()) {
			if (con.getConstraintType() == ConstraintType.PRIMARY_KEY && con.getColumns().contains(col)) {
				pk = true;
			}
		}
		return pk;
	}

	public boolean isColumnFK(Column col) {
		boolean fk = false;
		for (Constraint con : constraints.values()) {
			if (con.getConstraintType() == ConstraintType.FOREIGN_KEY && con.getColumns().contains(col)) {
				fk = true;
			}
		}
		return fk;
	}

	public boolean hasPK() {
		for (Column col : columns.values()) { 
			if (isColumnPK(col)) {
				return true;
			}
		}
				
		return false;
	}

	public boolean hasFK() {
		for (Column col : columns.values()) { 
			if (isColumnFK(col)) {
				return true;
			}
		}
				
		return false;
	}

}
