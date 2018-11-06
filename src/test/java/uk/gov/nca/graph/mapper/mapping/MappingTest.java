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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import uk.gov.nca.graph.mapper.exceptions.ParseException;

public class MappingTest {
    @Test
    public void testFromString() throws ParseException {
        Mapping m1 = Mapping.fromString("_STRING(1)");
        assertEquals(new Mapping(DataType.STRING, "1"), m1);

        Mapping m2 = Mapping.fromString("_DATE(date_field)");
        assertEquals(new Mapping(DataType.DATE, "date_field"), m2);

        Mapping m3 = Mapping.fromString("_LITERAL(Hello World)");
        assertEquals(new Mapping("Hello World"), m3);

        try{
            Mapping.fromString("Hello World");
            fail("Expected exception not thrown");
        }catch (ParseException pe){}
    }
}
