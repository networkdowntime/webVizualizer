package net.networkdowntime.vizualizer.api.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import net.networkdowntime.db.erdiagrams.ERDiagramCreator;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstraction;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory.DBType;
import net.networkdowntime.db.viewFilter.GraphFilter;

import org.springframework.stereotype.Component;


@Component
@Path("db/connectionFake")
public class ConnectionFake {

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getRoot() {
		StringBuffer sb = new StringBuffer();

		Path mainPath = this.getClass().getAnnotation(javax.ws.rs.Path.class);

		for (Method method : this.getClass().getMethods()) {
			Path path = method.getAnnotation(Path.class);

			if (path != null) {
				sb.append(mainPath.value() + path.value() + "<br>\n");
			}
		}
		return sb.toString();
	}

	@GET
	@Path("/supportedDatabases")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getSupportedDatabases() {
		
		List<String> types = new ArrayList<String>();
		types.add("MySql");
		types.add("Oracle");
		types.add("SqlServer");
		
		return types;
	}

	static DatabaseAbstraction dba;
	static ERDiagramCreator creator;

	@POST
	@Path("/connect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> getConnection(@FormParam("dbType") String dbType, @FormParam("username") String userName,
			@FormParam("password") String password, @FormParam("jdbcUrl") String url) {
		
		System.out.println("dbType: " + dbType);
		System.out.println("username: " + userName);
		System.out.println("password: " + password);
		System.out.println("jdbcUrl: " + url);
		
		HashMap<String, Object> response = new HashMap<String, Object>();

		return response;
	}

	List<String> response = new ArrayList<String>();

	@GET
	@Path("/scanSchemas")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void getScanSchemas(@QueryParam("schemas[]") List<String> schemasToScan, @Context HttpServletRequest request) {
		response = new ArrayList<String>();
		
		for (String schema : schemasToScan) {
			System.out.println(schema);
			
			if ("System".equals(schema)) {
				response.add("table1");
			} else if ("Test".equals(schema)) {
				response.add("table2");
			}
		}
	}

	@GET
	@Path("/schemasWithTables")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getSchemasWithTables() {
		List<String> response = new ArrayList<String>();
		response.add("System");
		response.add("Test");
		return response;
	}

	@GET
	@Path("/scannedTables")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getScannedTables() {
		return response;
	}

	@POST
	@Path("/dot")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getDot(GraphFilter filter) {
		System.out.println("includeTablesWithMoreXRows: " + filter.getIncludeTablesWithMoreXRows());
		System.out.println("Excluded Tables:");
		for (String s : filter.getTablesToExclude()) {
			System.out.println("\t" + s);
		}

		StringBuffer resp = new StringBuffer("graph G {\n");
		resp.append("run -- intr;\n");
		resp.append("intr -- runbl;\n");
		resp.append("runbl -- run;\n");
		resp.append("run -- kernel;\n");
		resp.append("kernel -- zombie;\n");
		resp.append("kernel -- sleep;\n");
		resp.append("kernel -- runmem;\n");
		resp.append("sleep -- swap;\n");
		resp.append("swap -- runswap;\n");
		resp.append("runswap -- new;\n");
		resp.append("runswap -- runmem;\n");
		resp.append("new -- runmem;\n");
		resp.append("sleep -- runmem;\n");
		resp.append("}");
		return resp.toString();
		
	}

}