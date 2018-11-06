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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import uk.gov.nca.graph.mapper.exceptions.ConfigurationException;
import uk.gov.nca.graph.mapper.mapping.DataType;
import uk.gov.nca.graph.mapper.mapping.EdgeMap;
import uk.gov.nca.graph.mapper.mapping.Mapping;
import uk.gov.nca.graph.mapper.mapping.VertexMap;

public class ConfigurationTest {

    @Test
    public void test() throws Exception{
        InputStream is = Configuration.class.getResourceAsStream("test.map");
        Configuration conf = Configuration.loadConfiguration(is);

        assertEquals(2, conf.getVertices().size());
        assertEquals(1, conf.getEdges().size());

        VertexMap vm1 = conf.getVertices().get(0);
        assertEquals("Person", vm1.getType());
        assertEquals(1, vm1.getId());
        assertFalse(vm1.getMerge());

        Map<String, List<Mapping>> props1 = vm1.getProperties();
        assertEquals(4, props1.size());
        assertEquals(new Mapping(DataType.STRING, "1"), props1.get("name").get(0));
        assertEquals(new Mapping(DataType.STRING, "2"), props1.get("gender").get(0));
        assertEquals(new Mapping(DataType.DATE, "3"), props1.get("dateOfBirth").get(0));
        assertEquals(new Mapping("example.txt"), props1.get("source").get(0));

        VertexMap vm2 = conf.getVertices().get(1);
        assertEquals("Person", vm2.getType());
        assertEquals(2, vm2.getId());
        assertTrue(vm2.getMerge());

        Map<String, List<Mapping>> props2 = vm2.getProperties();
        assertEquals(1, props2.size());
        List<Mapping> name = props2.get("name");
        assertEquals(new Mapping(DataType.STRING, "4"), name.get(0));
        assertEquals(new Mapping(" "), name.get(1));
        assertEquals(new Mapping(DataType.STRING, "5"), name.get(2));

        EdgeMap em = conf.getEdges().get(0);
        assertEquals("parentOf", em.getType());
        assertEquals(1, em.getSourceId());
        assertEquals(2, em.getTargetId());

        Map<String, Object> doesMatch = new HashMap<>();
        doesMatch.put("nationality", "British");
        doesMatch.put("name", "Sam");
        assertTrue(conf.matchesFilters(doesMatch));

        Map<String, Object> doesntMatchNationality = new HashMap<>();
        doesntMatchNationality.put("nationality", "American");
        doesntMatchNationality.put("name", "Sam");
        assertFalse(conf.matchesFilters(doesntMatchNationality));

        Map<String, Object> doesntMatchName = new HashMap<>();
        doesntMatchName.put("nationality", "British");
        assertFalse(conf.matchesFilters(doesntMatchName));
    }

    @Test
    public void testNullExcept() throws ConfigurationException {
        String map = "vertices:\n"
            + "- _type: Person\n"
            + "  name: _STRING(0)\n"
            + "  _except:\n"
            + "    name: null";

        Configuration conf = Configuration.loadConfiguration(new ByteArrayInputStream(map.getBytes()));

        assertEquals(1, conf.getVertices().size());

        VertexMap vm = conf.getVertices().get(0);
        assertTrue(vm.getExcept().containsKey("name"));
        assertNull(vm.getExcept().get("name"));
    }
}
