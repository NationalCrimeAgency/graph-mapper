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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source for reading from an SQL database
 */
public class SqlDataSource implements DataSource{

    private Connection conn = null;
    private ResultSet rs = null;
    private long count = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDataSource.class);

    public SqlDataSource(String jdbcConnection, String table, String username, String password, String query) throws SQLException{
        if (username != null && !username.isEmpty() && password != null) {
            LOGGER.info("Connecting to SQL database with username and password");
            conn = DriverManager.getConnection(jdbcConnection, username, password);
        }else{
            LOGGER.info("Connecting to SQL database without username and password");
            conn = DriverManager.getConnection(jdbcConnection);
        }

        PreparedStatement ps = conn.prepareStatement(query != null ? query : "SELECT * FROM `"+table+"`");
        rs = ps.executeQuery();
    }

    @Override
    public void close() throws Exception {
        if(conn != null){
            try{
                conn.close();
            }catch (SQLException e) {
                //Do nothing if there's an issue here
            }
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return (!rs.isLast() && ((rs.getRow() != 0) || rs.isBeforeFirst()));
        }catch (SQLException e){
            return false;
        }
    }

    @Override
    public Map<String, Object> next(){
        count++;
        if(count % 10000 == 0)
            LOGGER.info("Returning {}th row at {}", count, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        try {
            rs.next();
            Map<String, Object> data = new HashMap<>();

            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String key = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);

                data.put(key, value);
                data.put(Integer.toString(i), value);
            }

            return data;
        }catch (SQLException e){
            LOGGER.warn("Unable to get next row", e);
            throw new NoSuchElementException();
        }
    }
}
