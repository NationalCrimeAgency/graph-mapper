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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Test;

public class XmlDataSourceTest {
  @Test
  public void testXml() throws Exception{
    InputStream is = XmlDataSourceTest.class.getResourceAsStream("test.xml");

    try(
      XmlDataSource xds = new XmlDataSource(is, "chat");
    ){
      int count = 0;
      while(xds.hasNext()){
        Map<String, Object> map = xds.next();
        count++;

        System.out.println(map);

        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("from"));  //Check a top level node
        assertTrue(map.containsKey("#id"));  //Check a top level attribute
        assertTrue(map.containsKey("client.name")); //Check a nested node
        assertTrue(map.containsKey("client.name#type"));  //Check a nested attribute

        if("browser".equals(map.get("client.name#type"))){
          //Check merging a list
          assertTrue(map.containsKey("client.supports.format"));
          Object format = map.get("client.supports.format");
          assertTrue(format instanceof List);

          List<Object> l = (List<Object>) format;
          assertEquals(3, l.size());
          assertTrue(l.contains("json"));
        }
      }

      assertEquals(3, count);

      try{
        xds.next();
        fail("Expected exception not thrown");
      }catch (NoSuchElementException nsee){
        // Expected exception
      }
    }
  }
}
