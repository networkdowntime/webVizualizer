package net.networkdowntime.db.erdiagrams;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Representation of the Column in a Database
 *  
 * @author Ryan.Wiles
 *
 */
public @Data class Column {
	
	/**
	 * The name of the column
	 */
	private String name;
	
	/**
	 * Whether the field is nullable
	 */
	private boolean nullable;

	/**
	 * The default value of the column if any
	 */
	private String columnDefault;
	
	/**
	 * The data type of the column
	 */
	private String dataType;

	/**
	 * Contains the aggregation of other columns to show the formatted data type as used in DDL
	 */
	private String columnType;

	/**
	 * Identifies the position of the column in the table
	 */
	private int ordinalPosition;

	/**
	 * In MySql this is used for auto-increment
	 */
	private String extra; // used for auto_increment

	/**
	 * Any comments on the column
	 */
	private String comment;
	
	/**
	 * The length of the data type
	 */
	private BigDecimal length;
	
	/**
	 * If a floating point value, this indicates the number of digits
	 */
	private BigDecimal precision;
	
	/**
	 * If a floating point value, this indicates the number of decimal values
	 */
	private BigDecimal scale;

	public Column(String name) {
		this.name = name;
	}
}
