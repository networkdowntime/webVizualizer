package net.networkdowntime.javaAnalyzer.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.webVizualizer.dto.Status;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/code/javaScanner")
public class JavaScanner {

	static File file;
	Project project = new Project();

	@RequestMapping(value = "/files", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getFiles() {
		List<String> retval = new ArrayList<String>();
		for (File file : project.getFiles()) {
			retval.add(file.getPath());
		}
		return retval;
	}

	@RequestMapping(value = "/file", method = RequestMethod.GET, produces = { "text/html;charset=UTF-8" })
	@ResponseBody
	public String getFile(@RequestParam("class") String className, HttpServletResponse response) {

		System.out.println("class: " + className);

		String fileName = project.searchForFileOfClass(className);
		File f = new File(fileName);
		if (fileName != null && f.exists() && f.isFile()) {
			
			String header = readResourceFile("javaCodeHtmlHeader.txt");
			header = header.replaceAll("#TITLE_GOES_HERE", className);
			
			StringBuilder builder = new StringBuilder();
			builder.append(header);

			try {
				BufferedReader reader = new BufferedReader(new FileReader(f));
				while (reader.ready()) {
					builder.append(reader.readLine() + "<br>");
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			builder.append(readResourceFile("javaCodeHtmlFooter.txt"));
			return builder.toString();
		} else {
			response.setStatus(HttpStatus.NO_CONTENT.ordinal());
		}
		return "";
	}

	private String readResourceFile(String resourceName) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		File file = new File(classLoader.getResource(resourceName).getFile());
		StringBuilder builder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				builder.append(reader.readLine());
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}

	@RequestMapping(value = "/file", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	@ResponseStatus(value = HttpStatus.OK)
	public Status postFile(@RequestBody Map<String, String> body) {
		String path = body.get("path");
		System.out.println("path: " + path);

		File file = new File(path);

		boolean exists = file.exists();

		Status status = new Status(false);
		if (exists) {
			project.addFile(file);
			status.setSuccess(true);
		}
		return status;
	}

	@RequestMapping(value = "/file", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteFile(@RequestParam("path") String path, HttpServletResponse response) {

		System.out.println("path: " + path);
		boolean deleted = false;

		for (File file : project.getFiles()) {
			if (file.getPath().equals(path)) {
				project.removeFile(file);
				deleted = true;
			}
		}

		if (!deleted) {
			response.setStatus(HttpStatus.NOT_FOUND.ordinal());
		}
	}

	@RequestMapping(value = "/packages", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getPackages() {
		return project.getPackageNames();
	}

	@RequestMapping(value = "/classes", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getClasses() {
		return project.getClassNames(new ArrayList<String>());
	}

	@RequestMapping(value = "/classes", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	public List<String> postClasses(@RequestBody Map<String, Object> body) {
		List<String> excludePackages = (List<String>) body.get("excludePackages");
		System.out.println("excludePackages: " + excludePackages);
		if (excludePackages != null) {
			for (String s : excludePackages)
				System.out.println("\t" + s);

		}
		return project.getClassNames(excludePackages);
	}

	@RequestMapping(value = "/dot", method = RequestMethod.POST, produces = { "plain/text;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	@ResponseBody
	public String getDot(@RequestBody JavaFilter filter) {
		System.out.println("advancedSearchQuery: " + filter.getAdvancedSearchQuery());
		System.out.println("diagramType: " + filter.getDiagramType().toString());
		System.out.println("isShowFields: " + filter.isShowFields());
		System.out.println("isShowMethods: " + filter.isShowMethods());
		System.out.println("isFromFile: " + filter.isFromFile());
		System.out.println("getUpstreamReferenceDepth: " + filter.getUpstreamReferenceDepth());
		System.out.println("getDownstreamDependencyDepth: " + filter.getDownstreamDependencyDepth());
		System.out.println("Excluded Packages:");
		for (String s : filter.getPackagesToExclude()) {
			System.out.println("\t" + s);
		}
		System.out.println("Excluded Classes:");
		for (String s : filter.getClassesToExclude()) {
			System.out.println("\t" + s);
		}

		project.validate();
		String dot = project.createGraph(filter);
		
		return dot;
	}

}