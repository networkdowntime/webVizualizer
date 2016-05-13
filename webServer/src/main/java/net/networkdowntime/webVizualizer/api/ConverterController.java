package net.networkdowntime.webVizualizer.api;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@RestController
@RequestMapping("api/converter")
public class ConverterController {

	@POST
	@Path("/svgToPng")
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
}