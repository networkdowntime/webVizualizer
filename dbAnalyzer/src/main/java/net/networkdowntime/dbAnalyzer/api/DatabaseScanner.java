package net.networkdowntime.dbAnalyzer.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory;
import net.networkdowntime.dbAnalyzer.databases.DatabaseAbstractionFactory.DBType;
import net.networkdowntime.dbAnalyzer.dbModel.DatabaseWalker;
import net.networkdowntime.dbAnalyzer.dto.Schema;
import net.networkdowntime.dbAnalyzer.dto.Table;
import net.networkdowntime.dbAnalyzer.dto.Url;
import net.networkdowntime.dbAnalyzer.graphBuilder.ERDiagramCreator;
import net.networkdowntime.dbAnalyzer.graphBuilder.GraphBuilder;
import net.networkdowntime.dbAnalyzer.viewFilter.GraphFilter;
import net.networkdowntime.webVizualizer.dto.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private static final Logger LOGGER = LogManager.getLogger(DatabaseScanner.class.getName());

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

	@RequestMapping(value = "/connection", method = RequestMethod.POST, produces = { "application/json;charset=UTF-8" })
	public Status addConnection(@RequestBody Map<String, String> body) {
		String dbType = body.get("dbType");
		String userName = body.get("username");
		String password = body.get("password");
		String url = body.get("jdbcUrl");

		boolean added = false;
		if (DatabaseAbstractionFactory.getDatabaseAbstraction(DBType.valueOf(dbType), userName, password, url).testConnection().equals("success")) {
			dbWalker.addConnection(DBType.valueOf(dbType), userName, password, url);
			added = true;
		}
		return new Status(added);
	}

	@RequestMapping(value = "/connection", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteConnection(@RequestParam("url") String url, HttpServletResponse response) {
		boolean deleted = dbWalker.removeConnection(url);

		if (!deleted) {
			response.setStatus(HttpStatus.NOT_FOUND.ordinal());
		}
	}

	@RequestMapping(value = "/connections", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public Set<Url> getConnections() {
		TreeSet<Url> urls = new TreeSet<Url>((Url u1, Url u2) -> u1.getUrl().compareTo(u2.getUrl()));
		for (String urlString : dbWalker.getUrls()) {
			Url url = new Url();
			url.setUrl(urlString);
			List<Schema> schemas = new ArrayList<Schema>();
			url.setSchemas(schemas);

			for (String schemaName : dbWalker.getSchemas(urlString)) {
				Schema schema = new Schema();
				schema.setUrl(urlString);
				schema.setSchemaName(schemaName);
				List<Table> tables = new ArrayList<Table>();
				schema.setTables(tables);

				for (String tableName : dbWalker.getTables(urlString, schemaName)) {
					Table table = new Table();
					table.setUrl(urlString);
					table.setSchemaName(schemaName);
					table.setTableName(tableName);
					tables.add(table);
				}

				tables.sort((Table t1, Table t2) -> t1.getTableName().compareTo(t2.getTableName()));
				schemas.add(schema);
			}

			schemas.sort((Schema s1, Schema s2) -> s1.getSchemaName().compareTo(s2.getSchemaName()));
			url.setSchemas(schemas);
			urls.add(url);
		}
		return urls;
	}

	@RequestMapping(value = "/schemasWithTables", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public Map<String, Set<String>> getSchemasWithTables() {
		return dbWalker.getSchemas();
	}

	@RequestMapping(value = "/tables", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public Map<String, Set<String>> getScannedTables(@RequestParam("schemas[]") List<String> schemas) {
		Map<String, Set<String>> urlSchemaMap = new LinkedHashMap<String, Set<String>>();

		for (String str : schemas) {
			String url = str.substring(0, str.indexOf("."));
			String schema = str.substring(str.indexOf(".") + 1);
			LOGGER.debug(str + "; " + url + "; " + schema);

			Set<String> schemaNames;
			if (urlSchemaMap.containsKey(url)) {
				schemaNames = urlSchemaMap.get(url);
			} else {
				schemaNames = new LinkedHashSet<String>();
				urlSchemaMap.put(url, schemaNames);
			}
			schemaNames.add(schema);
		}

		return dbWalker.getTables(urlSchemaMap);
	}

	@RequestMapping(value = "/dot", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public GraphFilter getDot() {
		GraphFilter filter = new GraphFilter();
		return filter;
	}

	@RequestMapping(value = "/dot", method = RequestMethod.POST, produces = { "plain/text;charset=UTF-8" }, consumes = { "application/json;charset=UTF-8" })
	@ResponseBody
	public String postDot(@RequestBody GraphFilter filter) {
		LOGGER.debug("showAllColumnsOnTables: " + filter.isShowAllColumnsOnTables());
		LOGGER.debug("includeTablesWithMoreXRows: " + filter.getIncludeTablesWithMoreXRows());
		LOGGER.debug("Included Tables:");
		for (String s : filter.getTablesToInclude()) {
			LOGGER.debug("\t" + s);
		}
		LOGGER.debug("pkFilter: " + filter.getPkFilter().toString());
		LOGGER.debug("connectWithFKs: " + filter.isConnectWithFKs());
		LOGGER.debug("showLabelsOnFKs: " + filter.isShowLabelsOnFKs());
		LOGGER.debug("excludeFKForColumnsNamed: ");
		for (String s : filter.getExcludeFKForColumnsNamed()) {
			LOGGER.debug("\t" + s);
		}
		LOGGER.debug("excludeTablesContaining: ");
		for (String s : filter.getExcludeTablesContaining()) {
			LOGGER.debug("\t" + s);
		}
		LOGGER.debug("fkFilter: " + filter.getFkFilter().toString());

		GraphBuilder builder = new ERDiagramCreator();
		return builder.createGraph(dbWalker, filter);
	}

}