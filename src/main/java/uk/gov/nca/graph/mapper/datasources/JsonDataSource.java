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

package uk.gov.nca.graph.mapper.datasources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Data source for reading from a JSON array
 */
public class JsonDataSource implements DataSource{
    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<List<Map<String, Object>>>() {};

    private List<Map<String, Object>> objects;

    public JsonDataSource(String file) throws IOException{
        objects = mapper.readValue(new File(file), typeRef);
    }

    public JsonDataSource(InputStream stream) throws IOException{
        objects = mapper.readValue(stream, typeRef);
    }

    @Override
    public boolean hasNext() {
        return !objects.isEmpty();
    }

    @Override
    public Map<String, Object> next() {
        Map<String, Object> data;
        try{
            data = objects.remove(0);
        }catch (Exception e){
            throw new NoSuchElementException();
        }

        if(data == null)
            throw new NoSuchElementException();

        return data;
    }
}
