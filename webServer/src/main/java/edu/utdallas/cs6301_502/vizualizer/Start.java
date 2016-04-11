package edu.utdallas.cs6301_502.vizualizer;


import java.io.IOException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import java.io.File;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;


public class Start {

	private static int PORT_NUMBER = 8080;

	public static void main(String[] args) throws IOException {
		// Initialize Grizzly HttpServer
		// Taken from:
		// http://grizzly-nio.net/2013/08/grizzly-httpserver-spring-jersey-serve-static-content-from-a-folder-and-from-a-jar/

		HttpServer server = new HttpServer();
		NetworkListener listener = new NetworkListener("System Analyzer", "localhost", PORT_NUMBER);

		server.addListener(listener);

		// Initialize and add Spring-aware Jersey resource
		WebappContext ctx = new WebappContext("ctx", "/api");
		final ServletRegistration reg = ctx.addServlet("spring", new SpringServlet());
		reg.addMapping("/*");
		reg.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		reg.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
		//ctx.addContextInitParameter("contextConfigLocation", "file:src/main/resources/spring-context.xml");
		ctx.addContextInitParameter("contextConfigLocation", "classpath:spring-context.xml");
		ctx.addListener("org.springframework.web.context.ContextLoaderListener");
		ctx.addListener("org.springframework.web.context.request.RequestContextListener");
		ctx.deploy(server);

		// Add the StaticHttpHandler to serve static resources from the static1 folder
		File webapp = new File("src/main/webapp");
		System.out.println("webapp exists (src/main/webapp): " + webapp.exists());
		if (webapp.exists()) {
			server.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp/"), "/");
		} else {
			HttpHandler httpHandler = new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/webapp/");
			server.getServerConfiguration().addHttpHandler(httpHandler, "/");
		}
		
		// Add the CLStaticHttpHandler to serve static resources located at the static2 folder from the jar file
		// jersey1-grizzly2-spring-1.0-SNAPSHOT.jar
		// server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(new URLClassLoader(new URL[] { new File("target/jersey1-grizzly2-spring-1.0-SNAPSHOT.jar").toURI().toURL() }), "webapp/static2/"), "/jarstatic");

		try {
			server.start();

			System.out.println();
			System.out.println();
			System.out.println("In order to access the server please try the following urls:");
			System.out.println();
			System.out.println("http://localhost:" + PORT_NUMBER + "/index.html to see the index.html from the webapp folder");

			System.out.println();
			System.out.println("Press enter when you want to stop the server...");
			System.in.read();
		} finally {
			server.shutdownNow();
		}

	}
}
