package edu.utdallas.cs6301_502.vizualizer.api.code;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Project;
import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.JavaFilter;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value=WebApplicationContext.SCOPE_SESSION)
@Path("code/javaScanner")
public class JavaScanner {

	Project project = new Project();
	
	@POST
	@Path("/toPng")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("image/png")
	public void toPng(@Context HttpServletResponse response, @FormParam("svg") String svg) {
//		svg = "<svg  xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"><rect x=\"10\" y=\"10\" height=\"100\" width=\"100\" style=\"stroke:#ff0000; fill: #0000ff\"/></svg>";
		System.out.println(svg);
		try {
			response.setHeader("Content-Disposition", "attachment; filename=\"schema.png\"");
			response.setHeader("Content-Transfer-Encoding", "binary");
			
			// Step -1: We read the input SVG document into Transcoder Input
			// We use Java NIO for this purpose
			TranscoderInput input_svg_image = new TranscoderInput(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));

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

	@GET
	@Path("/files")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getFile() {
		List<String> retval = new ArrayList<String>();
		for (File file : project.getFiles()) {
			retval.add(file.getPath());
		}
		return retval;
	}

	
	@POST
	@Path("/file")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void postFile(@FormParam("path") String path, @Context final HttpServletResponse response) {

		System.out.println("path: " + path);

		File file = new File(path);

		boolean exists = file.exists();
		
		if (exists) {
			project.addFile(file);
		} else {
			response.setStatus(Response.Status.NOT_FOUND.ordinal());
		}
	}

	@DELETE
	@Path("/file")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void deleteFile(@FormParam("path") String path, @Context final HttpServletResponse response) {

		System.out.println("path: " + path);
		boolean deleted = false;
		
		for (File file : project.getFiles()) {
			if (file.getPath().equals(path)) {
				project.removeFile(file);
				deleted = true;
			}
		}
		
		if (deleted) {
			response.setStatus(Response.Status.NO_CONTENT.ordinal());
		} else {
			response.setStatus(Response.Status.NOT_FOUND.ordinal());
		}
	}


	@GET
	@Path("/packages")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getPackages() {
		return project.getPackageNames();
	}


	@GET
	@Path("/classes")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getClasses() {
		return project.getClassNames(new ArrayList<String>());
	}

	@POST
	@Path("/classes")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> postClasses(List<String> excludePackages) {
		System.out.println("excludePackages: " + excludePackages);
		if (excludePackages != null) {
			for (String s : excludePackages)
			System.out.println("\t" + s);
			
		}
		return project.getClassNames(excludePackages);
	}
	
	@POST
	@Path("/dot")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getDot(JavaFilter filter) {
		System.out.println("diagramType: " + filter.getDiagramType().toString());
		System.out.println("isShowFields: " + filter.isShowFields());
		System.out.println("isShowMethods: " + filter.isShowMethods());
		System.out.println("Excluded Packages:");
		for (String s : filter.getPackagesToExclude()) {
			System.out.println("\t" + s);
		}
		System.out.println("Excluded Classes:");
		for (String s : filter.getClassesToExclude()) {
			System.out.println("\t" + s);
		}

		project.validate();
		return project.createGraph(filter);
	}

}