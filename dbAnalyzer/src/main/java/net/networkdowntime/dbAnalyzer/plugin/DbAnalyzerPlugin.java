package net.networkdowntime.dbAnalyzer.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import net.networkdowntime.webVizualizer.plugin.PluginInterface;

public class DbAnalyzerPlugin implements PluginInterface {

	@Override
	public String getId() {
		return "dbAnalyzer";
	}

	@Override
	public String getLabel() {
		return "DB Analyzer";
	}

	@Override
	public String getJavaScript() {
		return readResourceFile("dbAnalyzer.js");
	}

	@Override
	public String getSideBar() {
		return readResourceFile("dbAnalyzerSidebar.html");
	}

	private String readResourceFile(String resourceName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				builder.append(reader.readLine());
				builder.append("\r\n");
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}


}
