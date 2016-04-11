package edu.utdallas.cs6301_502.javaAnalyzer.viewFilter;

import java.util.HashSet;

import org.codehaus.jackson.annotate.JsonProperty;

import lombok.Data;

/*
 * Package diagram of varying scope (selective inclusion of packages)
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

	public DiagramType getDiagramType() {
		return diagramType;
	}

	public void setDiagramType(DiagramType diagramType) {
		this.diagramType = diagramType;
	}

	public HashSet<String> getPackagesToExclude() {
		return packagesToExclude;
	}

	public void setPackagesToExclude(HashSet<String> packagesToExclude) {
		this.packagesToExclude = packagesToExclude;
	}

	public HashSet<String> getClassesToExclude() {
		return classesToExclude;
	}

	public void setClassesToExclude(HashSet<String> classesToExclude) {
		this.classesToExclude = classesToExclude;
	}

	public boolean isShowMethods() {
		return showMethods;
	}

	public void setShowMethods(boolean showMethods) {
		this.showMethods = showMethods;
	}

	public boolean isShowFields() {
		return showFields;
	}

	public void setShowFields(boolean showFields) {
		this.showFields = showFields;
	}

	public boolean isFromFile() {
		return fromFile;
	}

	public void setFromFile(boolean fromFile) {
		this.fromFile = fromFile;
	}

}
