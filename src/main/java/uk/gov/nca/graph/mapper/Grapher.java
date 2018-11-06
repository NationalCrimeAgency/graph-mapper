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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.nca.graph.mapper.exceptions.ParseException;
import uk.gov.nca.graph.mapper.mapping.DataType;
import uk.gov.nca.graph.mapper.mapping.DataTypeUtils;
import uk.gov.nca.graph.mapper.mapping.EdgeMap;
import uk.gov.nca.graph.mapper.mapping.Mapping;
import uk.gov.nca.graph.mapper.mapping.VertexMap;

/**
 * Class for adding data to a graph based on the configuration
 */
public class Grapher {
    private final Configuration configuration;

    private static final Logger LOGGER = LoggerFactory.getLogger(Grapher.class);
    private static final String IDENTIFIER = "identifier";

    /**
     * Constructor taking the configuration to use
     */
    public Grapher(Configuration configuration){
        this.configuration = configuration;
    }

    /**
     * Get the configuration of this class
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Add an index to the graph on IDENTIFIER, iff the graph is a TinkerGraph
     */
    public void addIndex(Graph graph){
        if(graph instanceof TinkerGraph){
            LOGGER.info("Creating index on field {}", IDENTIFIER);
            ((TinkerGraph) graph).createIndex(IDENTIFIER, Vertex.class);
            LOGGER.info("Index created on field {}", IDENTIFIER);
        }else{
            LOGGER.warn("Unable to create index on field {} as graph type {} is not supported",
                IDENTIFIER,
                graph.getClass().getSimpleName());
        }
    }

    /**
     * Perform mapping and add data to graph
     */
    public void addDataToGraph(Map<String, Object> data, Graph graph){
        addDataToGraph(data, graph, Collections.emptyMap());
    }

    /**
     * Perform mapping and add data to graph, adding the contents of auditData
     * to every vertex and edge. Audit data will override any data from the
     * mapping file, and any existing data in the graph.
     */
    public void addDataToGraph(Map<String, Object> data, Graph graph, Map<String, Object> auditData){
        Map<Object, Vertex> vertexMap = new HashMap<>();

        //First process vertices
        for(VertexMap vm : configuration.getVertices()){
            //Should we skip?
            if(shouldSkip(data, vm))
                continue;

            //Get properties
            Map<String, Object> properties = getProperties(data, vm, configuration.isLenient());

            //Add vertices
            Vertex v;
            if(vm.getMerge()){
                GraphTraversal<Vertex, Vertex> traversal = graph.traversal().V().hasLabel(vm.getType());
                for(Map.Entry<String, Object> e : properties.entrySet()){
                    traversal = traversal.has(e.getKey(), e.getValue());
                }

                List<Vertex> vertices = traversal.toList();

                if(!vertices.isEmpty()) {
                    v = vertices.get(0);
                }else {
                    v = getVertexFromGraph(vm.getType(), properties.get(IDENTIFIER), graph);
                }
            } else {
                v = getVertexFromGraph(vm.getType(), properties.get(IDENTIFIER), graph);
            }

            for(Map.Entry<String, Object> e : properties.entrySet()){
                v.property(e.getKey(), e.getValue());
            }

            for(Map.Entry<String, Object> e : auditData.entrySet()){
                v.property(e.getKey(), e.getValue());
            }

            //Add to map so we can use them for edges
            if(vm.getId() != null)
                vertexMap.put(vm.getId(), v);
        }

        //Add edges
        for(EdgeMap em : configuration.getEdges()){
            Vertex source = vertexMap.get(em.getSourceId());
            Vertex target = vertexMap.get(em.getTargetId());

            if(source == null || target == null)
                continue;

            Edge edge = source.addEdge(em.getType(), target);

            for(Map.Entry<String, Object> e : auditData.entrySet()){
                edge.property(e.getKey(), e.getValue());
            }
        }
    }

    private static boolean shouldSkip(Map<String, Object> data, VertexMap vertexMap){
        for(Map.Entry<String, Mapping> e : vertexMap.getExcept().entrySet()){
            Object val = data.get(e.getKey());

            Object exceptVal = null;
            Mapping m = e.getValue();
            if(m == null) {
                //Do nothing, exceptVal already null
            }else if(m.getDataType().equals(DataType.LITERAL)){
                exceptVal = m.getLiteral();
            }else{
                Object o = data.get(m.getField());
                if(o != null && !o.toString().isEmpty()) {
                    try {
                        exceptVal = DataTypeUtils.convert(o, m.getDataType());
                    } catch (ParseException pe) {
                        LOGGER.warn("Couldn't convert value of except {} from {} to type {}", e.getKey(), m.getField(), m.getDataType(), pe);
                    }
                }
            }

            if(Objects.equals(exceptVal, val))
                return true;
        }

        return false;
    }

    private static Map<String, Object> getProperties(Map<String, Object> data, VertexMap vertexMap, boolean lenient){
        Map<String, Object> properties = new HashMap<>();

        //Add properties
        for(Map.Entry<String, List<Mapping>> e : vertexMap.getProperties().entrySet()){
            List<Mapping> mappings = e.getValue();

            if(mappings.size() == 1){
                Mapping m = mappings.get(0);

                if(m.getDataType().equals(DataType.LITERAL)){
                    properties.put(e.getKey(), m.getLiteral());
                }else{
                    Object o = data.get(m.getField());
                    if(o == null || o.toString().isEmpty())
                        continue;

                    try {
                        Object mappedObj = DataTypeUtils.convert(o, m.getDataType());
                        properties.put(e.getKey(), mappedObj);
                    }catch (ParseException pe){
                      if(lenient){
                        //If lenient is true, and we weren't able to parse, convert the object to a String
                        properties.put(e.getKey(), o.toString());
                      }else {
                        LOGGER.warn("Couldn't convert data from {} to type {}", m.getField(),
                            m.getDataType(), pe);
                      }
                    }
                }
            }else{
                StringBuilder sb = new StringBuilder();

                boolean nonEmptyField = false;  //False if all fields (not LITERALs) are empty, true if any of them have a value
                boolean hasField = false;       //False if everything is a LITERAL, true if there is a field

                DataType type = DataType.STRING;
                Mapping firstMapping = mappings.get(0);
                if(firstMapping.getDataType() != DataType.LITERAL && firstMapping.getField().isEmpty()){
                    type = firstMapping.getDataType();
                    mappings.remove(0);
                }

                for(Mapping m : mappings){
                    if(m.getDataType().equals(DataType.LITERAL)){
                        sb.append(m.getLiteral());
                    }else{
                        hasField = true;

                        Object o = data.get(m.getField());
                        if(o == null || o.toString().isEmpty())
                            continue;

                        try {
                            Object mappedObj = DataTypeUtils.convert(o, m.getDataType());
                            sb.append(mappedObj.toString());
                            nonEmptyField = true;
                        }catch (ParseException pe){
                          if(lenient){
                            //If lenient is true, and we weren't able to parse, convert the object to a String
                            properties.put(e.getKey(), o.toString());
                          }else {
                            LOGGER.warn("Couldn't convert data from {} to type {}", m.getField(),
                                m.getDataType(), pe);
                          }
                        }
                    }
                }

                if(hasField && !nonEmptyField)  //There were fields, but they were all empty so skip
                    continue;

                if(type == DataType.STRING) {
                    properties.put(e.getKey(), sb.toString());
                }else{
                    try{
                        Object mappedObj = DataTypeUtils.convert(sb.toString(), type);
                        properties.put(e.getKey(), mappedObj);
                    } catch (ParseException pe) {
                        //If lenient is true, and we weren't able to parse, convert the object to a String
                        if(lenient) {
                            properties.put(e.getKey(), sb.toString());
                        }else{
                            LOGGER.warn("Couldn't convert list to type {}", type, pe);
                        }
                    }
                }
            }
        }

        return properties;
    }

    private static Vertex getVertexFromGraph(String type, Object identifier, Graph graph){
        if(identifier == null){
            return graph.addVertex(type);
        }

        List<Vertex> vertices = graph.traversal().V().has(IDENTIFIER, identifier).toList();
        for(Vertex v : vertices){
            if(type.equals(v.label()))
                return v;
        }

        return graph.addVertex(type);
    }
}
