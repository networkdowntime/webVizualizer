package net.networkdowntime.analyzer.api.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.networkdowntime.db.erdiagrams.ERDiagramCreator;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstraction;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory;
import net.networkdowntime.db.erdiagrams.database.DatabaseAbstractionFactory.DBType;
import net.networkdowntime.db.viewFilter.GraphFilter;

import org.springframework.stereotype.Component;


@Component
@Path("db/connection")
public class Connection {

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

	@GET
	@Path("/connect")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> getConnection(@QueryParam("dbType") String dbType, @QueryParam("username") String userName,
			@QueryParam("password") String password, @QueryParam("jdbcUrl") String url) {
		HashMap<String, Object> response = new HashMap<String, Object>();

		creator = new ERDiagramCreator();
		dba = creator.setConnection(DBType.valueOf(dbType), userName, password, url);

		return response;
	}

	@GET
	@Path("/scanSchemas")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void getScanSchemas(@QueryParam("schemas") List<String> schemasToScan) {
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

	@GET
	@Path("/dot")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDot() {
		GraphFilter filter = new GraphFilter();
		filter.addExcludeFKForColumnsNamed("CREATED_BY");
		filter.addExcludeFKForColumnsNamed("UPDATED_BY");
		filter.setConnectWithFKs(true);
		filter.setShowAllColumnsOnTables(false);
		filter.setShowLabelsOnFKs(false);
		filter.setIncludeTablesWithMoreXRows(1);
		return creator.createGraphvizString(filter);
	}

}