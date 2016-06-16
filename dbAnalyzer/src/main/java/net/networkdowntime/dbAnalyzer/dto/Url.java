package net.networkdowntime.dbAnalyzer.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Url implements Serializable {
	private static final long serialVersionUID = 3970369374477127442L;

	String url;
	List<Schema> schemas = new ArrayList<Schema>();

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Schema> getSchemas() {
		return schemas;
	}

	public void setSchemas(List<Schema> schemas) {
		this.schemas = schemas;
	}

}
