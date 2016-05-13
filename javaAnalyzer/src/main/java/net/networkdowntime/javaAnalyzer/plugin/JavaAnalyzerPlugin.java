package net.networkdowntime.javaAnalyzer.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import net.networkdowntime.webVizualizer.plugin.PluginInterface;

public class JavaAnalyzerPlugin implements PluginInterface {

	@Override
	public String getId() {
		return "javaAnalyzer";
	}

	@Override
	public String getLabel() {
		return "Java Analyzer";
	}

	@Override
	public String getJavaScript() {
		return readResourceFile("javaAnalyzer.js");
	}

	@Override
	public String getSideBar() {
		return readResourceFile("javaAnalyzerSidebar.html");
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
