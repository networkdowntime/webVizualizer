package net.networkdowntime.analyzer;

import java.io.IOException;

import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;


/**
 * The sample demonstrates how to make Jersey-Spring
 * integration work on top of Grizzly 2, including static pages served from
 * a folder or from within a jar file.
 * 
 * @author Alexey Stashok
 */
public class Start {

	private static int PORT_NUMBER = 8080;

	public static void main(String[] args) throws IOException {
		// Initialize Grizzly HttpServer
		// Taken from:
		// http://grizzly-nio.net/2013/08/grizzly-httpserver-spring-jersey-serve-static-content-from-a-folder-and-from-a-jar/

		HttpServer server = new HttpServer();
		NetworkListener listener = new NetworkListener("System Analyzer", "localhost", PORT_NUMBER);

		server.addListener(listener);

		// ServletContainer container = new ServletContainer();

		// String[] contexts = new String[] {"/api", "/api/db"};

		// Initialize and add Spring-aware Jersey resource
		WebappContext ctx = new WebappContext("ctx", "/api");
		final ServletRegistration reg = ctx.addServlet("spring", new SpringServlet());
		reg.addMapping("/*");
		reg.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		reg.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
		ctx.addContextInitParameter("contextConfigLocation", "file:src/main/resources/spring-context.xml");
		// ctx.addContextInitParameter("contextConfigLocation", "classpath:spring-context.xml");
		ctx.addListener("org.springframework.web.context.ContextLoaderListener");
		ctx.addListener("org.springframework.web.context.request.RequestContextListener");
		ctx.deploy(server);

		// Add the StaticHttpHandler to serve static resources from the static1 folder
		server.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp/"), "/");

		
		// Add the CLStaticHttpHandler to serve static resources located at the static2 folder from the jar file
		// jersey1-grizzly2-spring-1.0-SNAPSHOT.jar
		// server.getServerConfiguration().addHttpHandler(
		// new CLStaticHttpHandler(new URLClassLoader(new URL[] {
		// new File("target/jersey1-grizzly2-spring-1.0-SNAPSHOT.jar").toURI().toURL()}), "webapp/static2/"),
		// "/jarstatic");

		try {
			server.start();

			System.out.println("In order to test the server please try the following urls:");
			System.out.println("http://localhost:" + PORT_NUMBER + "/api to see the default TestResource.getIt() resource");
			System.out.println("http://localhost:" + PORT_NUMBER + "/api/test to see the TestResource.test() resource");
			System.out.println("http://localhost:" + PORT_NUMBER + "/api/test2 to see the TestResource.test2() resource");
			System.out.println("http://localhost:" + PORT_NUMBER + "/ to see the index.html from the webapp/static1 folder");
			System.out.println("http://localhost:" + PORT_NUMBER + "/jarstatic/ to see the index.html from the webapp/static2 folder served from the jar file");

			System.out.println();
			System.out.println("Press enter to stop the server...");
			System.in.read();
		} finally {
			server.shutdownNow();
		}

	}
}
