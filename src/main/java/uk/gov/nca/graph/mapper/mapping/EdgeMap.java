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

/**
 * Simple object to hold information about a edge mapping
 */
public class EdgeMap {
    private String type = null;
    private Object sourceId = null;
    private Object targetId = null;

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public Object getSourceId() {
        return sourceId;
    }
    public void setSourceId(Object sourceId) {
        this.sourceId = sourceId;
    }

    public Object getTargetId() {
        return targetId;
    }
    public void setTargetId(Object targetId) {
        this.targetId = targetId;
    }
}
