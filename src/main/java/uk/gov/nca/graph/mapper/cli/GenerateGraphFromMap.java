package uk.gov.nca.graph.mapper.cli;

import static uk.gov.nca.graph.utils.cli.CommandLineUtils.createRequiredOption;
import static uk.gov.nca.graph.utils.cli.CommandLineUtils.parseCommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.nca.graph.mapper.Configuration;
import uk.gov.nca.graph.mapper.GraphGenerator;
import uk.gov.nca.graph.mapper.exceptions.ConfigurationException;
import uk.gov.nca.graph.utils.GraphUtils;

public class GenerateGraphFromMap {
  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateGraphFromMap.class);

  public static void main(String[] args){
    //Configure command line parameters and parse
    Options options = new Options();

    options.addOption(createRequiredOption("c", "config", true, "Mapping configuration file"));
    options.addOption(createRequiredOption("g", "graph", true, "Tinkerpop graph configuration file (for output)"));

    CommandLine cmd = parseCommandLine(args, options, GenerateGraphFromMap.class, "Generate a sample graph from a mapping file");
    if(cmd == null)
      return;

    LOGGER.info("Reading mapping configuration file");
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

    LOGGER.info("Connecting to graph");
    Graph outputGraph = GraphFactory.open(cmd.getOptionValue('g'));

    LOGGER.info("Generating sample graph");
    GraphGenerator.generateGraph(outputGraph, conf);

    GraphUtils.commitGraph(outputGraph);

    LOGGER.info("Closing graph");
    //Close connection to output graph
    try {
      outputGraph.close();
    }catch(Exception e){
      LOGGER.error("Error occurred whilst closing output graph - graph may not have persisted correctly", e);
    }
  }
}
