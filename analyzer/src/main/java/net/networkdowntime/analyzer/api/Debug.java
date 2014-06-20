package net.networkdowntime.analyzer.api;

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

@Component
@Path("/debug")
public class Debug {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getIt() {
        return "Debug Page";
    }

    @GET
    @Path("/path")
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, Object> path() {
    	HashMap<String, Object> response = new HashMap<String, Object>();
    	response.put("path", System.getProperty("user.dir"));
        return response;
    }

    @GET
    @Path("/echo")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public HashMap<String, Object> echo(@QueryParam("input") String input) {
    	HashMap<String, Object> response = new HashMap<String, Object>();
    	response.put("output", input);
        return response;
    }

    
}