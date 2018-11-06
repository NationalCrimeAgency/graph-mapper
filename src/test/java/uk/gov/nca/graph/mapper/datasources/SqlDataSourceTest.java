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

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.junit.Test;

public class SqlDataSourceTest {

    private Connection conn;

    @Test
    public void testDatabase() throws Exception{
        setup();

        DataSource ds = new SqlDataSource("jdbc:h2:mem:test", "my_table", "user", null, null);

        assertTrue(ds.hasNext());
        Map<String, Object> row1 = ds.next();
        assertEquals(8, row1.size());

        assertEquals(1, row1.get("1"));
        assertEquals("Bob Smith", row1.get("2"));
        assertEquals(24, row1.get("3"));
        assertEquals("male", row1.get("4"));

        assertEquals(1, row1.get("ID"));
        assertEquals("Bob Smith", row1.get("NAME"));
        assertEquals(24, row1.get("AGE"));
        assertEquals("male", row1.get("GENDER"));

        assertTrue(ds.hasNext());
        Map<String, Object> row2 = ds.next();

        assertEquals(8, row2.size());

        assertEquals(2, row2.get("1"));
        assertEquals("Alice Jones", row2.get("2"));
        assertEquals(26, row2.get("3"));
        assertEquals("female", row2.get("4"));

        assertEquals(2, row2.get("ID"));
        assertEquals("Alice Jones", row2.get("NAME"));
        assertEquals(26, row2.get("AGE"));
        assertEquals("female", row2.get("GENDER"));

        assertFalse(ds.hasNext());

        ds.close();

        teardown();
    }

    private void setup() throws SQLException{
        conn = DriverManager.getConnection("jdbc:h2:mem:test");
        conn.prepareStatement("CREATE TABLE my_table (id int primary key, name varchar(128), age int, gender varchar(6))").execute();

        conn.prepareStatement("INSERT INTO my_table " +
                "(id, name, age, gender) VALUES " +
                "(1, 'Bob Smith', 24, 'male')").execute();

        conn.prepareStatement("INSERT INTO my_table " +
                "(id, name, age, gender) VALUES " +
                "(2, 'Alice Jones', 26, 'female')").execute();
    }

    private void teardown() throws SQLException{
        conn.prepareStatement("DROP TABLE my_table").execute();
        conn.close();
    }
}
