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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class IteratorDataSourceTest {

  @Test
  public void testIterator() throws Exception{
    List<String> list = Arrays.asList("a", "b");

    IteratorDataSource<String> ids = new IteratorDataSource<>(list.iterator(), s -> {
      Map<String, Object> map = new HashMap<>();
      map.put("value", s);
      return map;
    });

    assertTrue(ids.hasNext());
    Map<String, Object> m1 = ids.next();
    assertEquals("a", m1.get("value"));

    assertTrue(ids.hasNext());
    Map<String, Object> m2 = ids.next();
    assertEquals("b", m2.get("value"));

    assertFalse(ids.hasNext());

    ids.close();
  }
}
