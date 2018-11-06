/*
National Crime Agency (c) Crown Copyright 2018

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.gov.nca.graph.mapper;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;
import uk.gov.nca.graph.utils.ElementUtils;
import uk.gov.nca.graph.utils.GraphUtils;
import uk.gov.nca.graph.mapper.datasources.CsvDataSource;
import uk.gov.nca.graph.mapper.datasources.DataSource;
import uk.gov.nca.graph.mapper.datasources.JsonDataSource;

/**
 * Testing specific use cases to ensure they work as expected
 */
public class UseCasesTest {

  @Test
  public void testReplace() throws Exception{
    Graph graph = TinkerGraph.open();

    Configuration configuration = Configuration.loadConfiguration(UseCasesTest.class.getResourceAsStream("replace.map"));
    Grapher grapher = new Grapher(configuration);

    DataSource csvDataSource = new CsvDataSource(',', UseCasesTest.class.getResourceAsStream("replace.csv"), true);

    while(csvDataSource.hasNext()){
      Map<String, Object> row = csvDataSource.next();

      grapher.addDataToGraph(row, graph);
    }

    csvDataSource.close();

    File fOutput = new File("replace-output.graphml");
    fOutput.delete();

    GraphUtils.writeGraphFile(fOutput, "graphml", graph);

    //TODO: Programmatically validate output is correct

    graph.close();
    fOutput.delete();
  }

  @Test
  public void testMultipleLines() throws Exception{
    Graph graph = TinkerGraph.open();

    Configuration configuration = Configuration.loadConfiguration(UseCasesTest.class.getResourceAsStream("multiline.map"));
    Grapher grapher = new Grapher(configuration);

    DataSource csvDataSource = new JsonDataSource(UseCasesTest.class.getResourceAsStream("multiline.json"));

    while(csvDataSource.hasNext()){
      Map<String, Object> row = csvDataSource.next();

      grapher.addDataToGraph(row, graph);
    }

    csvDataSource.close();

    File fOutput = new File("multiline-output.graphml");
    fOutput.delete();

    GraphUtils.writeGraphFile(fOutput, "graphml", graph);

    graph.close();

    Graph outputGraph = TinkerGraph.open();
    GraphUtils.readGraphFile(fOutput, "graphml", outputGraph);

    List<Vertex> vertices = outputGraph.traversal().V().toList();
    assertEquals(1, vertices.size());

    Vertex v = vertices.get(0);
    assertEquals("Hello World", ElementUtils.getProperty(v, "headline"));
    assertEquals("This"+System.lineSeparator()+"Post"+System.lineSeparator()+"Has"+
        System.lineSeparator()+"Multiple Lines!", ElementUtils.getProperty(v, "text"));

    outputGraph.close();

    fOutput.delete();
  }
}
