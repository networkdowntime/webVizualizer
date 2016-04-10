package net.networkdowntime.db.erdiagrams;

import java.util.ArrayList;

import lombok.Data;


/**
 * Representation of the Constraint in a Database
 * 
 * @author Ryan.Wiles
 * 
 */
public @Data class Constraint {

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
}
