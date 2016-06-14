package net.networkdowntime.dbAnalyzer.viewFilter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;

import net.networkdowntime.dbAnalyzer.dbModel.Column;
import net.networkdowntime.dbAnalyzer.dbModel.Constraint;
import net.networkdowntime.dbAnalyzer.dbModel.ConstraintType;
import net.networkdowntime.dbAnalyzer.dbModel.Schema;
import net.networkdowntime.dbAnalyzer.dbModel.Table;


/**
 * The main filter class for database entity relation diagrams.
 * 
 * @author Ryan.Wiles
 * 
 */
@XmlRootElement
public class GraphFilter implements Serializable {
	private static final long serialVersionUID = 3970369374477127442L;

	/**
	 * Flag that instructs the filter to excludes columns that don't contain a primary key or foreign key constraint.
	 */
	boolean showAllColumnsOnTables = true;

	/**
	 * Allows you to filter tables based on the number of rows that they contain.<br>
	 * &nbsp;&nbsp;-1 show only empty tables<br>
	 * &nbsp;&nbsp;>=0 use number of rows in table for cut-off
	 */
	long includeTablesWithMoreXRows = 0;

	/**
	 * A list of the tables that you want to be excluded from the diagram.
	 * These should be schema.table format
	 */
	Set<String> tablesToInclude = new HashSet<String>(); 

	/**
	 * Allows you to filter tables based on whether or not they have a primary key.
	 */
	@JsonProperty("pkFilter")
	PkFilter pkFilter = PkFilter.All;

	/**
	 * Flag to indicate if you want to see the edges for foreign key constraints.
	 */
	boolean connectWithFKs = true;

	/**
	 * Flag to indicate if you want to see labels for the foreign key constraints.
	 */
	boolean showLabelsOnFKs = true;
	
	/**
	 * A list of column names for which you do not want to see the foreign key constraints.
	 */
	Set<String> excludeFKForColumnsNamed = new HashSet<String>();

	/**
	 * A list of column names for which you do not want to see the foreign key constraints.
	 */
	Set<String> excludeTablesContaining = new HashSet<String>();

	/**
	 * Allows you to filter tables based on whether or not they have a foreign key.
	 */
	@JsonProperty("fkFilter")
	FkFilter fkFilter = FkFilter.All;

	public GraphFilter() {
	}
	
	public void addExcludeFKForColumnsNamed(String columnName) {
		this.excludeFKForColumnsNamed.add(columnName);
	}

	public boolean skipTable(String url, Schema schema, Table table) {
		boolean skip = false;

		switch (pkFilter) {
		case HasPK:
			skip = skip || !table.hasPK();
			System.out.println("HasPK: skip = " + skip);
			break;
		case NoPK:
			skip = skip || table.hasPK();
			System.out.println("NoPK: skip = " + skip);
			break;
		default:
			break;
		}

		switch (fkFilter) {
		case HasFK:
			boolean containsNonSkippedFKConstraint = false;
			
			for (Constraint con : table.getConstraints().values()) {
				containsNonSkippedFKConstraint = containsNonSkippedFKConstraint || (!skipFKForConstraint(con)  && con.getConstraintType() == ConstraintType.FOREIGN_KEY);
			}

			skip = skip || !containsNonSkippedFKConstraint;

			System.out.println("HasFK: skip (" + table.getName() + ") = " + skip);
			break;
		case NoFK:
			skip = skip || table.hasFK();
			System.out.println("NoFK: skip (" + table.getName() + ") = " + skip);
			break;
		default:
			break;
		}

		if (!tablesToInclude.contains(url + "." + schema.getName() + "." + table.getName())) {
			skip = true;
		}
		
		for (String tableNamePart : excludeTablesContaining) {
			if (!tableNamePart.isEmpty() && table.getName().contains(tableNamePart)) {
				skip = true;
				System.out.println("excludeTablesContaining: skip = " + skip);
			}
		}

		if (includeTablesWithMoreXRows == -1) {
			skip = skip || table.getNumberOfRows().longValue() > 0l;
		} else {
			skip = skip || table.getNumberOfRows().longValue() < includeTablesWithMoreXRows;
		}

		return skip;
	}

	public boolean skipFKForConstraint(Constraint con) {
		boolean skip = false;

		for (Column col : con.getColumns()) {
			if (this.excludeFKForColumnsNamed.contains(col.getName()))
				skip = true;
		}

		return skip;
	}

	public boolean isShowAllColumnsOnTables() {
		return showAllColumnsOnTables;
	}

	public void setShowAllColumnsOnTables(boolean showAllColumnsOnTables) {
		this.showAllColumnsOnTables = showAllColumnsOnTables;
	}

	public long getIncludeTablesWithMoreXRows() {
		return includeTablesWithMoreXRows;
	}

	public void setIncludeTablesWithMoreXRows(long includeTablesWithMoreXRows) {
		this.includeTablesWithMoreXRows = includeTablesWithMoreXRows;
	}

	public Set<String> getTablesToInclude() {
		return tablesToInclude;
	}

	public void setTablesToInclude(Set<String> tablesToInclude) {
		this.tablesToInclude = tablesToInclude;
	}

	public PkFilter getPkFilter() {
		return pkFilter;
	}

	public void setPkFilter(PkFilter pkFilter) {
		this.pkFilter = pkFilter;
	}

	public boolean isConnectWithFKs() {
		return connectWithFKs;
	}

	public void setConnectWithFKs(boolean connectWithFKs) {
		this.connectWithFKs = connectWithFKs;
	}

	public boolean isShowLabelsOnFKs() {
		return showLabelsOnFKs;
	}

	public void setShowLabelsOnFKs(boolean showLabelsOnFKs) {
		this.showLabelsOnFKs = showLabelsOnFKs;
	}

	public Set<String> getExcludeFKForColumnsNamed() {
		return excludeFKForColumnsNamed;
	}

	public void setExcludeFKForColumnsNamed(Set<String> excludeFKForColumnsNamed) {
		this.excludeFKForColumnsNamed = excludeFKForColumnsNamed;
	}

	public Set<String> getExcludeTablesContaining() {
		return excludeTablesContaining;
	}

	public void setExcludeTablesContaining(Set<String> excludeTablesContaining) {
		this.excludeTablesContaining = excludeTablesContaining;
	}

	public FkFilter getFkFilter() {
		return fkFilter;
	}

	public void setFkFilter(FkFilter fkFilter) {
		this.fkFilter = fkFilter;
	}

}
