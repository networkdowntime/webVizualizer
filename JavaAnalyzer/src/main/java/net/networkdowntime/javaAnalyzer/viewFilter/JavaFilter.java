package net.networkdowntime.javaAnalyzer.viewFilter;

import java.util.HashSet;

import org.codehaus.jackson.annotate.JsonProperty;

import lombok.Data;

/* Package diagram of varying scope (selective inclusion of packages)
 * CAD of varying scope (selective inclusion of classes within packages)
 * Include/Exclude methods
 * Include/Exclude fields
 * Method CALL diagrams (from a single method, show me all classes/methods that get called and what they call)
 * Automatic detection of WS end points. (I want to be able to see the method flow diagram for a WS)
 * Automatic detection of outgoing calls to other systems (recognition of common libraries to perform SOAP and REST calls)
 * Automatic detection of DB calls using JDBC and JPA 
 * Being able to determine statically referenced tables in JDBC and JPA and map them to their corresponding tables from the DB Analyzer
 * Find unused classes and methods
 * Find unreferenced database tables
 */


public @Data class JavaFilter {

	@JsonProperty("diagramType")
	DiagramType diagramType = DiagramType.CLASS_ASSOCIATION_DIAGRAM;
	
	HashSet<String> packagesToExclude = new HashSet<String>();
	HashSet<String> classesToExclude = new HashSet<String>();
	
	boolean showMethods = false;
	boolean showFields = false;
	boolean fromFile = false;
}
