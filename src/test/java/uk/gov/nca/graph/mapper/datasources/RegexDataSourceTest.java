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

import com.google.re2j.Pattern;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Test;

public class RegexDataSourceTest {

  @Test
  public void testRegex(){
    String log = "My event log\n2018-04-16 - Event 1\n2018-04-17 - Event 2\nNot an event";
    Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}) - (.*)");

    RegexDataSource rds = new RegexDataSource(log, p);

    assertTrue(rds.hasNext());
    Map<String, Object> m1 = rds.next();
    assertEquals(3, m1.size());
    assertEquals("2018-04-16 - Event 1", m1.get("0"));
    assertEquals("2018-04-16", m1.get("1"));
    assertEquals("Event 1", m1.get("2"));

    assertTrue(rds.hasNext());
    Map<String, Object> m2 = rds.next();
    assertEquals(3, m2.size());
    assertEquals("2018-04-17 - Event 2", m2.get("0"));
    assertEquals("2018-04-17", m2.get("1"));
    assertEquals("Event 2", m2.get("2"));

    assertFalse(rds.hasNext());

    try{
      rds.next();
      fail("Expected exception not thrown");
    }catch (NoSuchElementException nsee){
      // Expected exception
    }
  }
}
