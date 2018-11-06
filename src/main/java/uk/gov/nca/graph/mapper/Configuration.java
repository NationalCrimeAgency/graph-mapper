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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import uk.gov.nca.graph.mapper.exceptions.ConfigurationException;
import uk.gov.nca.graph.mapper.exceptions.ParseException;
import uk.gov.nca.graph.mapper.mapping.EdgeMap;
import uk.gov.nca.graph.mapper.mapping.Mapping;
import uk.gov.nca.graph.mapper.mapping.VertexMap;

/**
 * Configuration object, which holds a list of VertexMap and EdgeMaps
 */
public class Configuration {
    private List<VertexMap> vertices = new ArrayList<>();
    private List<EdgeMap> edges = new ArrayList<>();
    private Map<String, Object> filters = new HashMap<>();

    private boolean isLenient = false;

    public static final String ID = "_id";
    public static final String TYPE = "_type";
    public static final String SOURCE = "_src";
    public static final String TARGET = "_tgt";
    public static final String MERGE = "_merge";
    public static final String EXISTS = "_exists";
    public static final String EXCEPT = "_except";
    public static final String LENIENT = "isLenient";

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    private static final String LOG_MESSAGE_EDGE_PROPERTY_REQUIRED = "{} property is required for all edges";

    /**
     * Load configuration from an InputStream
     */
    public static Configuration loadConfiguration(InputStream configuration) throws ConfigurationException {
        Configuration conf = new Configuration();

        Yaml yaml = new Yaml();
        Map<String, Object> configMap = (Map<String, Object>) yaml.load(configuration);

        try {
            conf.setFilters((Map<String, Object>) configMap.getOrDefault("filters", Collections.emptyMap()));
        }catch (ClassCastException cce){
            LOGGER.error("Unable to parse filters - no filters will be set", cce);
            conf.setFilters(Collections.emptyMap());
        }

        try {
            conf.loadVerticesConfiguration((List<Map<String, Object>>) configMap.get("vertices"));
        }catch (ClassCastException cce){
            throw new ConfigurationException("Unable to parse vertices", cce);
        }

        try {
            conf.loadEdgesConfiguration((List<Map<String, Object>>)configMap.get("edges"));
        }catch (ClassCastException cce){
            throw new ConfigurationException("Unable to parse edges", cce);
        }

        if(configMap.containsKey(LENIENT)){
            Object o = configMap.get(LENIENT);
            conf.setLenient(Boolean.valueOf(o.toString()));
        }

        return conf;
    }

    /**
     * Load configuration from a Map
     */
    public static Configuration loadConfiguration(Map<String, Object> configuration) throws ConfigurationException {
        Configuration conf = new Configuration();

        try {
            conf.setFilters((Map<String, Object>) configuration.getOrDefault("filters", Collections.emptyMap()));
        }catch (ClassCastException cce){
            LOGGER.error("Unable to parse filters - no filters will be set", cce);
            conf.setFilters(Collections.emptyMap());
        }

        try {
            conf.loadVerticesConfiguration((List<Map<String, Object>>) configuration.get("vertices"));
        }catch (ClassCastException cce){
            throw new ConfigurationException("Unable to parse vertices", cce);
        }

        try {
            conf.loadEdgesConfiguration((List<Map<String, Object>>)configuration.get("edges"));
        }catch (ClassCastException cce){
            throw new ConfigurationException("Unable to parse edges", cce);
        }

        if(configuration.containsKey(LENIENT)){
            Object o = configuration.get(LENIENT);
            conf.setLenient(Boolean.valueOf(o.toString()));
        }

        return conf;
    }

    public List<VertexMap> getVertices() {
        return vertices;
    }
    public List<EdgeMap> getEdges() {
        return edges;
    }

    private void setFilters(Map<String, Object> filters){
        this.filters = filters;
    }

    /**
     * Load vertices from the YAML file, and create the correct VertexMap mapping
     */
    private void loadVerticesConfiguration(List<Map<String, Object>> listVertices) throws ConfigurationException{
        if(listVertices == null)
            throw new ConfigurationException("Could not find vertices object");

        vertices.clear();

        for(Map<String, Object> vertex : listVertices){
            VertexMap vm = new VertexMap();

            //Check we have a type
            if(!vertex.containsKey(TYPE)){
                LOGGER.warn("{} property is required for all vertices", TYPE);
                continue;
            }

            //Should we be adding this as an ID, the Type or a generic property?
            for(Entry<String, Object> entry : vertex.entrySet()){
                switch (entry.getKey()){
                    case ID:
                        vm.setId(vertex.get(ID));
                        break;
                    case TYPE:
                        vm.setType(vertex.get(TYPE).toString());
                        break;
                    case MERGE:
                        vm.setMerge(vertex.get(MERGE).toString().equalsIgnoreCase("true"));
                        break;
                    case EXCEPT:
                        Map<String, Object> exceptConf;
                        try {
                            exceptConf = (Map<String, Object>) vertex.getOrDefault(EXCEPT, Collections.emptyMap());
                        }catch (ClassCastException cce){
                            LOGGER.warn("Expecting a map for {}", EXCEPT, cce);
                            break;
                        }

                        Map<String, Mapping> exceptMapping = new HashMap<>();

                        for(Map.Entry<String, Object> e : exceptConf.entrySet()){
                            try {
                                if(e.getValue() != null) {
                                    exceptMapping.put(e.getKey(),
                                        Mapping.fromString(e.getValue().toString()));
                                }else{
                                    exceptMapping.put(e.getKey(), null);
                                }
                            }catch (ParseException pe){
                                exceptMapping.put(e.getKey(), new Mapping(e.getValue()));
                            }
                        }

                        vm.setExcept(exceptMapping);

                        break;
                    default:
                        List<Object> list;

                        if(entry.getValue() instanceof List){
                            list = (List<Object>)entry.getValue();
                        }else{
                            list = new ArrayList<>();
                            list.add(entry.getValue());
                        }

                        for(Object obj : list){
                            try {
                                vm.addProperty(entry.getKey(), Mapping.fromString(obj.toString()));
                            }catch(ParseException pe){
                                vm.addProperty(entry.getKey(), new Mapping(obj.toString()));
                            }
                        }
                }
            }

            vertices.add(vm);
        }
    }

    /**
     * Load edges from the YAML file, and create the correct EdgeMap mapping
     */
    private void loadEdgesConfiguration(List<Map<String, Object>> listEdges) {
        edges.clear();

        if(listEdges == null)
            return;

        for(Map<String, Object> edge : listEdges){
            EdgeMap em = new EdgeMap();

            //Check we have all the required properties
            boolean requiredPropertiesPresent = true;

            if(!edge.containsKey(TYPE)){
                LOGGER.warn(LOG_MESSAGE_EDGE_PROPERTY_REQUIRED, TYPE);
                requiredPropertiesPresent = false;
            }
            if(!edge.containsKey(SOURCE)){
                LOGGER.warn(LOG_MESSAGE_EDGE_PROPERTY_REQUIRED, SOURCE);
                requiredPropertiesPresent = false;
            }
            if(!edge.containsKey(TARGET)){
                LOGGER.warn(LOG_MESSAGE_EDGE_PROPERTY_REQUIRED, TARGET);
                requiredPropertiesPresent = false;
            }

            if(!requiredPropertiesPresent)
                continue;

            //Set values
            em.setType(edge.get(TYPE).toString());
            em.setSourceId(edge.get(SOURCE));
            em.setTargetId(edge.get(TARGET));

            edges.add(em);
        }
    }

    /**
     * Returns true if the provided data matches the current set of filters.
     * Data must match all filters to return true.
     */
    public boolean matchesFilters(Map<String, Object> data){
        if(filters.isEmpty())
            return true;

        for(Map.Entry<String, Object> e : filters.entrySet()){
            //Check _exists
            if(e.getKey().equals(EXISTS)){
                if(e.getValue() instanceof List){
                    for(Object o : (List<?>) e.getValue()){
                        if(!data.containsKey(o.toString()))
                            return false;
                    }
                }else{
                    if(!data.containsKey(e.getValue().toString()))
                        return false;
                }

                continue;
            }

            //Compare values if the special cases above haven't been triggered
            Object val = data.get(e.getKey());

            if(e.getValue() instanceof List && !(val instanceof List)){
                List<?> filterVals = (List<?>) e.getValue();
                if(!filterVals.contains(val))
                    return false;

            }else{
                if(!e.getValue().equals(val))
                    return false;
            }
        }

        return true;
    }

    public boolean isLenient() {
        return isLenient;
    }

    public void setLenient(boolean lenient) {
        this.isLenient = lenient;
    }
}
