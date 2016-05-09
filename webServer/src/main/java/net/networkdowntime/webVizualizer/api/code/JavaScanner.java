package net.networkdowntime.webVizualizer.api.code;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.networkdowntime.javaAnalyzer.javaModel.Project;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.webVizualizer.dto.Status;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@RestController
@RequestMapping("api/code/javaScanner")
public class JavaScanner {

	static File file;
	Project project = new Project();

	@POST
	@Path("/toPng")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("image/png")
	public void toPng(@Context HttpServletResponse response, @FormParam("svg") String svg) {
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\"schema.png\"");
			response.setHeader("Content-Transfer-Encoding", "binary");

			String parser = XMLResourceDescriptor.getXMLParserClassName();
			SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
			factory.setValidating(false);
			Document svgDocument = factory.createDocument("http://networkdowntime.net", new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
			Element svgRoot = svgDocument.getDocumentElement();

			String widthStr = svgRoot.getAttributeNS(null, "width");
			String heightStr = svgRoot.getAttributeNS(null, "height");

			int width = Integer.parseInt(widthStr.replace("pt", "").trim());
			int height = Integer.parseInt(heightStr.replace("pt", "").trim());
			double aspectRatio = width / (double) height;

			if (width > 12500 || height > 12500) {
				if (width > height) {
					width = 12500; 
					height = (int) Math.round(width / aspectRatio); 
				} else {
					height = 12500;
					width = (int) Math.round(aspectRatio * height);
				}
				svgRoot.setAttribute("width", width + "pt");
				svgRoot.setAttribute("height", height + "pt");
			}

			// Step-1: We read the input SVG document into Transcoder Input
			// We use Java NIO for this purpose
			TranscoderInput input_svg_image = new TranscoderInput(svgDocument);
//			Document doc = input_svg_image.getDocument();

			// Step-2: Define OutputStream to PNG Image and attach to TranscoderOutput
			OutputStream png_ostream = response.getOutputStream();
			TranscoderOutput output_png_image = new TranscoderOutput(png_ostream);

			// Step-3: Create PNGTranscoder and define hints if required
			PNGTranscoder my_converter = new PNGTranscoder();

			// Step-4: Convert and Write output
			my_converter.transcode(input_svg_image, output_png_image);

			// Step 5- close / flush Output Stream
			png_ostream.flush();
			png_ostream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/files", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getFiles() {
		List<String> retval = new ArrayList<String>();
		for (File file : project.getFiles()) {
			retval.add(file.getPath());
		}
		return retval;
	}

	@RequestMapping(value = "/file", method = RequestMethod.GET, produces = { "plain/text;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	@ResponseBody
	public String getFile(@QueryParam("class") String className, HttpServletResponse response) {

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
			response.setStatus(Response.Status.NOT_FOUND.ordinal());
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