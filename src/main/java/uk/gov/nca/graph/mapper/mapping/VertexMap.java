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

package uk.gov.nca.graph.mapper.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple object to hold information about a vertex mapping
 */
public class VertexMap {
    private Object id = null;
    private String type = null;
    private boolean merge = false;
    private Map<String, Mapping> except = Collections.emptyMap();

    private Map<String, List<Mapping>> properties = new HashMap<>();

    public Object getId() {
        return id;
    }
    public void setId(Object id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Mapping> getExcept() {
        return except;
    }
    public void setExcept(Map<String, Mapping> except) {
        this.except = except;
    }

    public boolean getMerge() {
        return merge;
    }
    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public Map<String, List<Mapping>> getProperties() {
        return properties;
    }
    public void setProperties(Map<String, List<Mapping>> properties) {
        this.properties = properties;
    }
    public List<Mapping> getProperty(String property){
        return properties.get(property);
    }
    public void setProperty(String property, List<Mapping> mapping){
        properties.put(property, mapping);
    }
    public void addProperty(String property, Mapping mapping){
        List<Mapping> mappings = properties.getOrDefault(property, new ArrayList<>());
        mappings.add(mapping);
        properties.put(property, mappings);
    }


}
