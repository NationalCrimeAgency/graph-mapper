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
import static uk.gov.nca.graph.mapper.mapping.DataTypeUtils.convert;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Test;
import uk.gov.nca.graph.mapper.exceptions.ParseException;

public class DataTypeUtilsTest {
    @Test
    public void test() throws Exception{
        assertEquals("Hello world", convert("Hello world", DataType.LITERAL));
        assertEquals(47, convert(47, DataType.LITERAL));
        assertEquals(true, convert(true, DataType.LITERAL));

        assertEquals("Hello world", convert("Hello world", DataType.STRING));
        assertEquals("47", convert(47, DataType.STRING));
        assertEquals("true", convert(true, DataType.STRING));

        assertEquals(true, convert(true, DataType.BOOLEAN));
        assertEquals(true, convert("true", DataType.BOOLEAN));
        assertEquals(true, convert("YES", DataType.BOOLEAN));
        assertEquals(false, convert(false, DataType.BOOLEAN));
        assertEquals(false, convert("false", DataType.BOOLEAN));
        assertEquals(false, convert("no", DataType.BOOLEAN));
        assertEquals(false, convert("Hello", DataType.BOOLEAN));

        assertEquals(47, convert(47, DataType.INTEGER));
        assertEquals(47, convert("47", DataType.INTEGER));
        try{
            convert("Hello", DataType.INTEGER);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        assertEquals(47.0, convert(47, DataType.DOUBLE));
        assertEquals(47.0, convert(47.0, DataType.DOUBLE));
        assertEquals(47.0, convert("47.0", DataType.DOUBLE));
        try{
            convert("Hello", DataType.DOUBLE);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        LocalDate ld = LocalDate.of(2018, Month.JANUARY, 15);
        assertEquals(ld, convert(ld, DataType.DATE));
        assertEquals(ld, convert("2018-01-15", DataType.DATE));
        assertEquals(ld, convert("15-Jan-2018", DataType.DATE));
        assertEquals(ld, convert(Timestamp.from(ld.atStartOfDay().toInstant(ZoneOffset.UTC)), DataType.DATE));

        try{
            convert("Hello", DataType.DATE);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        ZonedDateTime zdt = ZonedDateTime.of(2018, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(zdt, convert(zdt, DataType.DATETIME));
        assertEquals(zdt, convert("2018-01-15 12:00:00", DataType.DATETIME));
        assertEquals(zdt, convert("2018-01-15T12:00:00+00:00", DataType.DATETIME));
        assertEquals(zdt, convert(zdt.toEpochSecond(), DataType.DATETIME));
        assertEquals(zdt, convert(zdt.toEpochSecond()*1000, DataType.DATETIME));
        assertEquals(zdt, convert(Timestamp.from(zdt.toInstant()), DataType.DATETIME));
        assertEquals(zdt, convert("Mon, 15 Jan 2018 12:00:00 GMT", DataType.DATETIME));

        try{
            convert("Hello", DataType.DATETIME);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        LocalTime lt = LocalTime.of(12, 0, 0);
        assertEquals(lt, convert(lt, DataType.TIME));
        assertEquals(lt, convert("12:00:00", DataType.TIME));
        try{
            convert("Hello", DataType.TIME);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        URL url = new URL("http://www.gov.uk");
        assertEquals(url, convert(url, DataType.URL));
        assertEquals(url, convert("http://www.gov.uk", DataType.URL));
        try{
            convert("Hello", DataType.URL);
            fail("Expected exception not thrown");
        }catch (ParseException pe){
            //Do nothing, expected
        }

        byte[] ip = new byte[4];
        ip[0] = 127;
        ip[1] = 0;
        ip[2] = 0;
        ip[3] = 1;

        assertEquals("127.0.0.1", convert("127.0.0.1", DataType.IPADDRESS));
        assertEquals("127.0.0.1", convert(ip, DataType.IPADDRESS));
        assertEquals("Hello", convert("Hello", DataType.IPADDRESS));
    }
}
