package net.networkdowntime.dbAnalyzer.viewFilter;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A filter class to include or exclude tables based on whether they have a foreign key.
 * 
 * @author Ryan.Wiles
 * 
 */
@XmlRootElement
public enum FkFilter implements Serializable {
	/**
	 * NoFK indicates that you want to filter out tables that have a foreign key.
	 */
	NoFK, 
	
	/**
	 * HasFK indicates that you want to filter out all tables that don't have a foreign key.
	 */
	HasFK, 
	
	/**
	 * All indicates that you do not want any filtering done for the foreign key.
	 */
	All
}