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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.RFC4180Parser;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Data source for reading from CSV files
 */
public class CsvDataSource implements DataSource {
    private List<String[]> lines = new ArrayList<>();
    private String[] colTitles;

    public CsvDataSource(char separator, String file, boolean header) throws IOException{
        this(separator, new FileReader(file), header);
    }

    public CsvDataSource(char separator, InputStream stream, boolean header) throws IOException{
        this(separator, new InputStreamReader(stream), header);
    }

    public CsvDataSource(char separator, Reader sourceReader, boolean header) throws IOException{
        ICSVParser parser;
        if(separator == ',') {
            parser = new RFC4180Parser();
        }else{
            parser = new CSVParserBuilder()
                .withEscapeChar(CSVParser.DEFAULT_ESCAPE_CHARACTER)
                .withQuoteChar(CSVParser.DEFAULT_QUOTE_CHARACTER)
                .withSeparator(separator).build();
        }
        CSVReader reader = null;

        try {
            reader = new CSVReaderBuilder(sourceReader).withCSVParser(parser).build();

            //If the header switch was enabled, then read in column headers
            colTitles = new String[0];
            if (header) {
                colTitles = reader.readNext();
            }

            //Read through file
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                lines.add(nextLine);
            }

        } finally {
            if(reader != null){
                reader.close();
            }
        }
    }

    @Override
    public boolean hasNext() {
        return !lines.isEmpty();
    }

    @Override
    public Map<String, Object> next() {
        String[] line = null;
        try{
            line = lines.remove(0);
        }catch (Exception e){
            throw new NoSuchElementException();
        }

        if(line == null)
            throw new NoSuchElementException();

        Map<String, Object> data = new HashMap<>();

        Integer count = 0;
        for (String col : line) {   //TODO: Can we cast values to the correct type (e.g. to Integer)
            if (colTitles.length > count) {
                data.put(colTitles[count], col);
            }

            data.put(Integer.toString(count + 1), col);
            count++;
        }

        return data;
    }
}
