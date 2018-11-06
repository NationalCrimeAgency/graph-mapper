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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source for reading from a JSON Lines file
 */
public class JsonLinesDataSource implements DataSource{
    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

    private List<String> lines = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLinesDataSource.class);

    public JsonLinesDataSource(String file) throws IOException{
        try(
            Stream<String> lineStream = Files.lines(Paths.get(file))
        ){
            lineStream.forEach(lines::add);
        }
    }

    public JsonLinesDataSource(InputStream stream) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line = null;

        while((line = in.readLine()) != null) {
            lines.add(line);
        }
    }

    @Override
    public boolean hasNext() {
        return !lines.isEmpty();
    }

    @Override
    public Map<String, Object> next() {
        String line = null;
        try{
            line = lines.remove(0);
        }catch (Exception e){
            throw new NoSuchElementException();
        }

        if(line == null)
            throw new NoSuchElementException();

        try {
            return mapper.readValue(line, typeRef);
        }catch (IOException e){
            LOGGER.warn("Unable to parse line", e);
            return Collections.emptyMap();
        }
    }
}
