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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.andreinc.mockneat.MockNeat;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import uk.gov.nca.graph.mapper.mapping.DataType;
import uk.gov.nca.graph.mapper.mapping.Mapping;

/**
 * Utility class to generate a sample graph based on a mapping file.
 * The content of the graph will be non-sensical, but the structure
 * will match that of the mapping file.
 */
public class GraphGenerator {
  private static MockNeat mock = MockNeat.threadLocal();

  private GraphGenerator(){
    //Private constructor for utility class
  }

  /**
   * Populate the provided graph with a graph that is structurally equivalent to the
   * graph described in the configuration.
   */
  public static void generateGraph(Graph graph, Configuration configuration){
    Map<Object, Vertex> vertices = new HashMap<>();
    configuration.getVertices().forEach(vm -> {
      Vertex v = graph.addVertex(vm.getType());
      vm.getProperties().entrySet().forEach(entry -> v.property(entry.getKey(), generateData(entry.getValue())));
      vertices.put(vm.getId(), v);
    });

    configuration.getEdges().forEach(em -> {
      Vertex vSrc = vertices.get(em.getSourceId());
      Vertex vTgt = vertices.get(em.getTargetId());

      vSrc.addEdge(em.getType(), vTgt);
    });
  }

  /**
   * Generate some random mock data for a given list of mappings
   */
  public static Object generateData(List<Mapping> spec){
    if(spec.isEmpty())
      return null;

    if(spec.size() == 1){
      return generateData(spec.get(0));
    }else{
      Mapping firstMapping = spec.get(0);
      if(firstMapping.getDataType() != DataType.LITERAL && firstMapping.getField().isEmpty()){
        return generateData(firstMapping);
      }else{
        return generateData(new Mapping(DataType.STRING, null));
      }
    }
  }

  /**
   * Generate some random mock data for a given mapping
   */
  public static Object generateData(Mapping spec){
    switch (spec.getDataType()){
      case LITERAL:
        return spec.getLiteral();
      case STRING:
        return mock.strings().val();
      case BOOLEAN:
        return mock.bools().val();
      case DATE:
        return mock.localDates().val();
      case INTEGER:
        return mock.ints().val();
      case DATETIME:
        return mock.localDates().val().atTime(randomLocalTime());
      case DOUBLE:
        return mock.doubles().val();
      case URL:
        String url = mock.urls().val();
        try {
          return new URL(url);
        }catch (MalformedURLException mue){
          return url;
        }
      case TIME:
        return randomLocalTime();
      case IPADDRESS:
        return mock.ipv4s().val();
    }

    return null;
  }

  private static LocalTime randomLocalTime(){
    ThreadLocalRandom tlr = ThreadLocalRandom.current();
    return LocalTime.of(tlr.nextInt(24), tlr.nextInt(60), tlr.nextInt(60));
  }
}
