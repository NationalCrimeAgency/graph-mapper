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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.nca.graph.mapper.mapping.DataType;
import uk.gov.nca.graph.mapper.mapping.Mapping;

public class GraphGeneratorTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(GraphGeneratorTest.class);

  @Test
  public void testGenerateData(){
    Mapping mLiteral = new Mapping("Hello World");
    assertEquals("Hello World", GraphGenerator.generateData(mLiteral));

    for(DataType dataType : DataType.values()){
      if(dataType == DataType.LITERAL)
        continue;

      Mapping m = new Mapping(dataType, "fieldName");
      Object o = GraphGenerator.generateData(m);

      assertNotNull(o);
      LOGGER.debug("Generated value for type {}: {}", dataType, o);
    }
  }

  @Test
  public void testGenerateList(){
    Mapping mLiteral = new Mapping("Hello World");
    Mapping mUrl = new Mapping(DataType.URL, "fieldName");
    Mapping mUrlEmpty = new Mapping(DataType.URL, "");

    //Empty list, return null
    assertNull(GraphGenerator.generateData(Collections.emptyList()));

    //Single object, should treat it not as a list
    assertEquals("Hello World", GraphGenerator.generateData(Collections.singletonList(mLiteral)));

    //Multiple objects with field, should return String
    assertEquals(String.class, GraphGenerator.generateData(Arrays.asList(mUrl, mLiteral)).getClass());

    //Multiple objects with Literal first, should return String
    assertEquals(String.class, GraphGenerator.generateData(Arrays.asList(mLiteral, mUrl)).getClass());

    //Multiple objects without field, should return URL
    assertEquals(URL.class, GraphGenerator.generateData(Arrays.asList(mUrlEmpty, mLiteral)).getClass());
  }
}
