package net.networkdowntime.db.erdiagrams;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

public @Data class Schema {
	
	/**
	 * The name of this Schema
	 */
	private String name;
	
	/**
	 * A map of the tables that are in this Schema
	 */
	private Map<String, Table> tables = new LinkedHashMap<String, Table>();

    public Schema(String name) {
        this.name = name;
    }

    public void addTable(String name, String comment) {
        tables.put(name, new Table(name, comment));
    }

    public void addTable(String name, String comment, BigDecimal numberOfRows) {
        tables.put(name, new Table(name, comment, numberOfRows));
    }
}
