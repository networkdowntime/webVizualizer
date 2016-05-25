package net.networkdowntime.dbAnalyzer.dbModel;

import java.util.ArrayList;

/**
 * Representation of the Constraint in a Database
 * 
 * @author Ryan.Wiles
 * 
 */
public class Constraint {

	private String name;

	/**
	 * Identifies the {@link Schema} that this constraint belongs to
	 */
	private Schema refSchema;

	/**
	 * Used to tie the foreign key constraint to a {@link Table}
	 */
	private Table refTable;

	/**
	 * Used to tie the foreign key constraint to the {@link Column} it references
	 */
	private ArrayList<Column> refColumn = new ArrayList<Column>();

	/**
	 * Used for unique and primary key constraints to identify which columns they link to
	 */
	private ArrayList<Column> columns = new ArrayList<Column>();

	/**
	 * The {@link ConstraintType} of the Constraint
	 */
	private ConstraintType constraintType;

	public Constraint(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Schema getRefSchema() {
		return refSchema;
	}

	public void setRefSchema(Schema refSchema) {
		this.refSchema = refSchema;
	}

	public Table getRefTable() {
		return refTable;
	}

	public void setRefTable(Table refTable) {
		this.refTable = refTable;
	}

	public ArrayList<Column> getRefColumn() {
		return refColumn;
	}

	public void setRefColumn(ArrayList<Column> refColumn) {
		this.refColumn = refColumn;
	}

	public ArrayList<Column> getColumns() {
		return columns;
	}

	public void setColumns(ArrayList<Column> columns) {
		this.columns = columns;
	}

	public ConstraintType getConstraintType() {
		return constraintType;
	}

	public void setConstraintType(ConstraintType constraintType) {
		this.constraintType = constraintType;
	}

}
