package net.networkdowntime.analyzer.api.db;

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

@Component
@Path("db/connection")
public class Connection {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getIt() {
        return "hello world";
    }

    @GET
    @Path("/path")
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, Object> test() {
    	HashMap<String, Object> response = new HashMap<String, Object>();
    	
    	response.put("path", System.getProperty("user.dir"));
        return response;
    }

}