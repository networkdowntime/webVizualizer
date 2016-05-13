package net.networkdowntime.webVizualizer.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import net.networkdowntime.webVizualizer.dto.Plugin;
import net.networkdowntime.webVizualizer.plugin.PluginInterface;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/plugin")
public class PluginController {

	static String[] pluginClasses = new String[] {
			"net.networkdowntime.dbAnalyzer.plugin.DbAnalyzerPlugin",
			"net.networkdowntime.javaAnalyzer.plugin.JavaAnalyzerPlugin"
	};

	static Map<String, PluginInterface> plugins = new HashMap<String, PluginInterface>();

	static {
		for (String className : pluginClasses) {
			try {
				plugins.put(className, create(className, PluginInterface.class));
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@RequestMapping(value = "/plugins", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<Plugin> getPlugins() {
		List<Plugin> retval = new ArrayList<Plugin>();
		for (PluginInterface plugin : plugins.values()) {
			retval.add(new Plugin(plugin.getId(), plugin.getLabel(), plugin.getClass().getCanonicalName()));
		}
		return retval;
	}

	// http://localhost:8080/api/plugin/javaScript?pluginClass=net.networkdowntime.javaAnalyzer.plugin.JavaAnalyzerPlugin
	@RequestMapping(value = "/javaScript", method = RequestMethod.GET, produces = { "application/javascript;charset=UTF-8" })
	@ResponseBody
	public String getJavaScript(@RequestParam("pluginClass") String pluginClass, HttpServletResponse response) {
		String retval = "";

		PluginInterface plugin = plugins.get(pluginClass);
		if (plugin != null) {
			retval = plugin.getJavaScript();
		} else {
			retval = "Class not found for " + pluginClass;
			response.setStatus(Response.Status.NOT_FOUND.ordinal());
		}

		return retval;
	}

	// http://localhost:8080/api/plugin/sideBar?pluginClass=net.networkdowntime.javaAnalyzer.plugin.JavaAnalyzerPlugin
	@RequestMapping(value = "/sideBar", method = RequestMethod.GET, produces = { "plain/text;charset=UTF-8" })
	@ResponseBody
	public String getSideBar(@RequestParam("pluginClass") String pluginClass, HttpServletResponse response) {
		String retval = "";

		PluginInterface plugin = plugins.get(pluginClass);
		if (plugin != null) {
			retval = plugin.getSideBar();
		} else {
			retval = "Class not found for " + pluginClass;
			response.setStatus(Response.Status.NOT_FOUND.ordinal());
		}

		return retval;
	}

	public static <T> T create(final String className, Class<T> interfaceClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		System.out.println("create: " + className);
		@SuppressWarnings("unchecked")
		final Class<T> clazz = (Class<T>) Class.forName(className);

		System.out.println("got clazz: " + className);
		return clazz.newInstance();
	}
}