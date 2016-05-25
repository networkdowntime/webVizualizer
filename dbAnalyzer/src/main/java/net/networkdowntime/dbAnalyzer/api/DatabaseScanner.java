package net.networkdowntime.dbAnalyzer.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstraction;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory.DBType;
import net.networkdowntime.dbAnalyzer.dbModel.DatabaseWalker;
import net.networkdowntime.dbAnalyzer.graphBuilder.ERDiagramCreator;
import net.networkdowntime.dbAnalyzer.graphBuilder.GraphBuilder;
import net.networkdowntime.dbAnalyzer.viewFilter.GraphFilter;
import net.networkdowntime.webVizualizer.dto.Status;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db/dbScanner")
public class DatabaseScanner {

//	private static DatabaseAbstraction dba;
	private static GraphBuilder creator;
	
	private static DatabaseWalker dbWalker = new DatabaseWalker();

	@RequestMapping(value = "/supportedDatabases", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getSupportedDatabases() {

		List<String> types = new ArrayList<String>();

		for (DBType type : DatabaseAbstractionFactory.DBType.values()) {
			types.add(type.toString());
		}
		return types;
	}

	@RequestMapping(value = "/testConnection", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" })
	public Status testConnection(@RequestBody Map<String, String> body) {
		String dbType = body.get("dbType");
		String userName = body.get("username");
		String password = body.get("password");
		String url = body.get("jdbcUrl");
		
		return new Status(DatabaseAbstractionFactory.getDatabaseAbstraction(DBType.valueOf(dbType), userName, password, url).testConnection().equals("success"));
	}

	@RequestMapping(value = "/addConnection", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" })
	public Status getConnection(@RequestBody Map<String, String> body) {
		String dbType = body.get("dbType");
		String userName = body.get("username");
		String password = body.get("password");
		String url = body.get("jdbcUrl");
		
		creator = new ERDiagramCreator();
//		dba = creator.setConnection(DBType.valueOf(dbType), userName, password, url);
		dbWalker.addConnection(DBType.valueOf(dbType), userName, password, url);
		dbWalker = new DatabaseWalker(dba, new ArrayList<String>());

		return new Status(dba.testConnection().equals("success"));
	}

	@RequestMapping(value = "/scanSchemas", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	public void getScanSchemas(@RequestParam("schemas[]") List<String> schemasToScan) {

		for (String schema : schemasToScan) {
			System.out.println("Schema To Scan: " + schema);
		}
		creator.analyzeDatabase(dba, schemasToScan);
	}

	@RequestMapping(value = "/schemasWithTables", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getSchemasWithTables() {
		return dba.getAllSchemaNamesWithTables();
	}

	@RequestMapping(value = "/scannedTables", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public List<String> getScannedTables() {
		return creator.getAllScannedTables();
	}

	@RequestMapping(value = "/dot", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public GraphFilter getDot() {
		GraphFilter filter = new GraphFilter();
		return filter;
	}
	
	@RequestMapping(value = "/dot", method = RequestMethod.POST, produces = { "plain/text;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	@ResponseBody
	public String postDot(@RequestBody GraphFilter filter) {
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
		System.out.println("excludeTablesContaining: ");
		for (String s : filter.getExcludeTablesContaining()) {
			System.out.println("\t" + s);
		}
		System.out.println("fkFilter: " + filter.getFkFilter().toString());

		return creator.createGraph(dbWalker, filter);
	}

}