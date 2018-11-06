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

import java.io.File;
import java.util.Map;
import org.junit.Test;

public class CsvDataSourceTest {

    @Test
    public void testCsv() throws Exception{
        File f = new File(getClass().getResource("test.csv").toURI());
        DataSource ds = new CsvDataSource(',', f.getPath(), true);

        assertData(ds);

        ds.close();
    }

    @Test
    public void testTsv() throws Exception{
        File f = new File(getClass().getResource("test.tsv").toURI());
        DataSource ds = new CsvDataSource('\t', f.getPath(), true);

        assertData(ds);

        ds.close();
    }

    private void assertData(DataSource ds){
        assertTrue(ds.hasNext());
        Map<String, Object> row1 = ds.next();
        assertEquals(8, row1.size());

        assertEquals("Bob", row1.get("1"));
        assertEquals("Smith", row1.get("2"));
        assertEquals("24", row1.get("3"));
        assertEquals("male", row1.get("4"));

        assertEquals("Bob", row1.get("firstname"));
        assertEquals("Smith", row1.get("surname"));
        assertEquals("24", row1.get("age"));
        assertEquals("male", row1.get("gender"));

        assertTrue(ds.hasNext());
        Map<String, Object> row2 = ds.next();

        assertEquals(8, row2.size());

        assertEquals("Alice", row2.get("1"));
        assertEquals("Jones", row2.get("2"));
        assertEquals("26", row2.get("3"));
        assertEquals("female", row2.get("4"));

        assertEquals("Alice", row2.get("firstname"));
        assertEquals("Jones", row2.get("surname"));
        assertEquals("26", row2.get("age"));
        assertEquals("female", row2.get("gender"));

        assertFalse(ds.hasNext());
    }
}
