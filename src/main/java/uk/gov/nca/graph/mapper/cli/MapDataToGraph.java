package uk.gov.nca.graph.mapper.cli;

import static uk.gov.nca.graph.utils.cli.CommandLineUtils.createRequiredOption;
import static uk.gov.nca.graph.utils.cli.CommandLineUtils.printHelp;

import com.google.re2j.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpHost;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.nca.graph.mapper.Configuration;
import uk.gov.nca.graph.mapper.Grapher;
import uk.gov.nca.graph.mapper.datasources.CsvDataSource;
import uk.gov.nca.graph.mapper.datasources.DataSource;
import uk.gov.nca.graph.mapper.datasources.ElasticDataSource;
import uk.gov.nca.graph.mapper.datasources.JsonDataSource;
import uk.gov.nca.graph.mapper.datasources.JsonLinesDataSource;
import uk.gov.nca.graph.mapper.datasources.RegexDataSource;
import uk.gov.nca.graph.mapper.datasources.SqlDataSource;
import uk.gov.nca.graph.mapper.datasources.XmlDataSource;
import uk.gov.nca.graph.mapper.exceptions.ConfigurationException;
import uk.gov.nca.graph.utils.GraphUtils;

//TODO: Tests

/**
 * Main entry point for application, which converts a data file (CSV, TSV or JSON) and
 * converts it into a graph.
 */
public class MapDataToGraph {
  public static final String PROVENANCE_KEY = "_p";

  private static final Logger LOGGER = LoggerFactory.getLogger(MapDataToGraph.class);

  public static void main(String[] args){
    //Configure command line parameters and parse
    Options options = new Options();

    options.addOption(createRequiredOption("c", "config", true, "Mapping configuration file"));
    options.addOption(createRequiredOption("d", "data", true, "Input file, or JDBC connection string if format is SQL, or Elasticsearch URL if the format is ES"));
    options.addOption(createRequiredOption("g", "graph", true, "Tinkerpop graph configuration file (for output)"));

    options.addOption("f", "format", true, "Input data format (CSV, TSV, JSON, JSONL, SQL, XML, REGEX, ES) [default CSV]");
    options.addOption("h", "headers", false, "CSV file has headers [default false]");
    options.addOption("t", "table", true, "Table name (required if format is SQL), or comma-separated indices (if the format is ES)");
    options.addOption("q", "query", true, "SQL query to execute (overrides table), or RegEx pattern, or Elasticsearch query");
    options.addOption("e", "element", true, "Element name (required if format is XML)");
    options.addOption("i", "ignorecase", false, "Ignore case in regular expressions [default false]");
    options.addOption("u", "username", true, "Username for SQL connections");   //TODO: Move this into a configuration file?
    options.addOption("p", "password", true, "Password for SQL connections");   //TODO: Move this into a configuration file?
    options.addOption("prov", true, "Provenance key to add to all data");
    options.addOption("a", "flatten", false, "Flatten data from nested format");
    CommandLine cmd = parseCommandLine(options, args);

    if(cmd == null)
      return;

    //Load Mapping Configuration
    Configuration conf;
    try (InputStream is = new FileInputStream(cmd.getOptionValue('c'))){
      conf = Configuration.loadConfiguration(is);
    } catch (IOException ioe) {
      LOGGER.error("Couldn't read mapping configuration {}", cmd.getOptionValue('c'), ioe);
      return;
    } catch (ConfigurationException ce){
      LOGGER.error("Couldn't parse mapping configuration", ce);
      return;
    }

    //Connect to graph
    LOGGER.info("Connecting to graph");
    Graph g;
    try{
      g = GraphFactory.open(cmd.getOptionValue('g'));
    }catch(Exception iae){
      LOGGER.error("Unable to connect to graph", iae);
      return;
    }

    //Connect to data source
    DataSource dataSource = getDataSource(cmd);

    Map<String, Object> auditData = getAuditData(cmd);

    //Load data into graph
    LOGGER.info("Beginning load of data into graph");
    Grapher grapher = new Grapher(conf);
    grapher.addIndex(g);

    boolean flatten = cmd.hasOption('a');

    long count = 0;
    if(dataSource != null){
      while(dataSource.hasNext()){
        count++;
        if(count % 1000 == 0)
          LOGGER.info("Processing record {}", count);

        Map<String, Object> data = dataSource.next();

        if (conf.matchesFilters(data))
          grapher.addDataToGraph(data, g, auditData, flatten);
      }
    }
    LOGGER.info("Done loading data into graph - {} data records loaded", count);

    //Disconnect from data source
    LOGGER.info("Disconnecting from data source");
    try{
      if(dataSource != null)
        dataSource.close();
    }catch (Exception e){
      //Do nothing
    }

    //Disconnect from graph
    LOGGER.info("Disconnecting from graph");
    GraphUtils.closeGraph(g);

    LOGGER.info("Finished");
  }

  private static CommandLine parseCommandLine(Options options, String[] args){
    CommandLineParser clParser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = clParser.parse(options, args);

      if("SQL".equalsIgnoreCase(cmd.getOptionValue('f')) && !(cmd.hasOption('t') || cmd.hasOption('q'))){
        cmd = null;
        throw new ParseException("Table name or query not specified");
      }

      if("XML".equalsIgnoreCase(cmd.getOptionValue('f')) && !cmd.hasOption('e')){
        cmd = null;
        throw new ParseException("Element name not specified");
      }

      if("REGEX".equalsIgnoreCase(cmd.getOptionValue('f')) && !cmd.hasOption('q')){
        cmd = null;
        throw new ParseException("Regex pattern not specified");
      }

    } catch (ParseException e) {
      printHelp(MapDataToGraph.class.getName(), "Map structured data into a graph", options);
    }

    return cmd;
  }

  private static DataSource getDataSource(CommandLine cmd){
    //Connect to data source
    String format = "CSV";
    if(cmd.hasOption('f'))
      format = cmd.getOptionValue('f').toUpperCase();

    DataSource dataSource = null;

    LOGGER.info("Connecting to {} data source", format);

    if(format.equals("JSON")) {
      try {
        dataSource = new JsonDataSource(cmd.getOptionValue('d'));
      } catch (IOException ioe) {
        LOGGER.error("Unable to initialise JSON data source", ioe);
      }
    }else if(format.equals("JSONL")){
      try {
        dataSource = new JsonLinesDataSource(cmd.getOptionValue('d'));
      }catch (IOException ioe){
        LOGGER.error("Unable to initialise JSON Lines data source", ioe);
      }
    }else if(format.equals("CSV")){
      try {
        dataSource = new CsvDataSource(',', cmd.getOptionValue('d'), cmd.hasOption('h'));
      }catch (IOException ioe){
        LOGGER.error("Unable to initialise CSV data source", ioe);
      }
    }else if(format.equals("TSV")){
      try {
        dataSource = new CsvDataSource('\t', cmd.getOptionValue('d'), cmd.hasOption('h'));
      }catch (IOException ioe){
        LOGGER.error("Unable to initialise TSV data source", ioe);
      }
    }else if(format.equals("SQL")){
      try {
        if(cmd.hasOption('q'))
          LOGGER.info("Filtering data with query: {}", cmd.getOptionValue('q'));
        dataSource = new SqlDataSource(cmd.getOptionValue('d'), cmd.getOptionValue('t'), cmd.getOptionValue('u'), cmd.getOptionValue('p'), cmd.getOptionValue('q'));
      }catch (SQLException ioe){
        LOGGER.error("Unable to initialise SQL data source", ioe);
      }
    }else if(format.equals("XML")){
      try {
        dataSource = new XmlDataSource(cmd.getOptionValue('d'), cmd.getOptionValue('e'));
      }catch (IOException | XMLStreamException e){
        LOGGER.error("Unable to initialise XML data source", e);
      }
    }else if(format.equals("REGEX")){
      try {
        if(cmd.hasOption('i')) {
          dataSource = new RegexDataSource(new File(cmd.getOptionValue('d')), Pattern
              .compile(cmd.getOptionValue('q'), Pattern.CASE_INSENSITIVE));
        }else{
          dataSource = new RegexDataSource(new File(cmd.getOptionValue('d')), Pattern.compile(cmd.getOptionValue('q')));
        }
      }catch (IOException e){
        LOGGER.error("Unable to initialise RegEx data source", e);
      }
    }else if(format.equals("ES")){
      HttpHost httpHost = HttpHost.create(cmd.getOptionValue('d'));

      SearchRequest searchRequest = new SearchRequest();
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      if(cmd.hasOption('q')){
        searchSourceBuilder.query(QueryBuilders.wrapperQuery(cmd.getOptionValue('q')));
      }else {
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
      }
      searchRequest.source(searchSourceBuilder);

      if(cmd.hasOption('t')){
        String[] indices = cmd.getOptionValue('t').split(",");
        searchRequest.indices(indices);
      }

      dataSource = new ElasticDataSource(httpHost, cmd.getOptionValue('u'), cmd.getOptionValue('p'), searchRequest);
    }

    return dataSource;
  }

  private static Map<String, Object> getAuditData(CommandLine cmd){
    Map<String, Object> auditData;
    if(cmd.hasOption("prov")){
      auditData = new HashMap<>();
      auditData.put(PROVENANCE_KEY, cmd.getOptionValue("prov"));
    }else{
      auditData = Collections.emptyMap();
    }

    return auditData;
  }
}