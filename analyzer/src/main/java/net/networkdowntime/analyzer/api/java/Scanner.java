package net.networkdowntime.analyzer.api.java;

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
import javax.ws.rs.core.Response;

import net.networkdowntime.db.erdiagrams.ERDiagramCreator;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstraction;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory.DBType;
import net.networkdowntime.db.viewFilter.GraphFilter;

import org.springframework.stereotype.Component;


@Component
@Path("code/java")
public class Scanner {

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

		for (DBType type : DatabaseAbstractionFactory.DBType.values()) {
			types.add(type.toString());
		}
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

		creator = new ERDiagramCreator();
		dba = creator.setConnection(DBType.valueOf(dbType), userName, password, url);

		dba.testConnection();

		return response;
	}

	@GET
	@Path("/scanSchemas")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void getScanSchemas(@QueryParam("schemas[]") List<String> schemasToScan, @Context HttpServletRequest request) {

		for (String schema : schemasToScan) {
			System.out.println("Schema To Scan: " + schema);
		}
		creator.analyzeDatabase(dba, schemasToScan);
	}

	@GET
	@Path("/schemasWithTables")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getSchemasWithTables() {
		return dba.getAllSchemaNamesWithTables();
	}

	@GET
	@Path("/scannedTables")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getScannedTables() {
		return creator.getAllScannedTables();
	}

	@POST
	@Path("/dot")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getDot(GraphFilter filter) {
		System.out.println("showAllColumnsOnTables: " + filter.isShowAllColumnsOnTables());
		System.out.println("includeTablesWithMoreXRows: " + filter.getIncludeTablesWithMoreXRows());
		System.out.println("Excluded Tables:");
		for (String s : filter.getTablesToExclude()) {
			System.out.println("\t" + s);
		}
		System.out.println("pkFilter: " + filter.getPkFilter().toString());
		System.out.println("connectWithFKs: " + filter.isConnectWithFKs());
		System.out.println("showLabelsOnFKs: " + filter.isShowLabelsOnFKs());
		System.out.println("excludeFKForColumnsNamed: ");
		for (String s : filter.getExcludeFKForColumnsNamed()) {
			System.out.println("\t" + s);
		}
		System.out.println("fkFilter: " + filter.getFkFilter().toString());

		return creator.createGraphvizString(filter);
	}

}