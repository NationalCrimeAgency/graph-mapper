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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import uk.gov.nca.graph.mapper.exceptions.ParseException;

/**
 * Utility class for working with DataTypes
 */
public class DataTypeUtils {
    private DataTypeUtils(){}

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Convert object to the given data type, and throw a ParseException if the object can't be converted
     */
    public static Object convert(Object data, DataType dataType) throws ParseException {
        switch (dataType){
            case LITERAL:
                return data;
            case STRING:
                if(data instanceof String)
                    return data;

                return data.toString();
            case BOOLEAN:
                if(data instanceof Boolean)
                    return data;

                return data.toString().equalsIgnoreCase("true") || data.toString().equalsIgnoreCase("yes");
            case INTEGER:
                if(data instanceof Integer)
                    return data;

                try{
                    return Integer.parseInt(data.toString());
                }catch (NumberFormatException nfe){
                    throw new ParseException("Couldn't parse Integer", nfe);
                }
            case DOUBLE:
                if(data instanceof Double)
                    return data;

                try{
                    return Double.parseDouble(data.toString());
                }catch (NumberFormatException nfe){
                    throw new ParseException("Couldn't parse Double", nfe);
                }
            case DATE:
                if(data instanceof LocalDate)
                    return data;

                if(data instanceof Timestamp)
                    return ((Timestamp)data).toLocalDateTime().toLocalDate();

                return parseDate(data);
            case URL:
                if(data instanceof URL)
                    return data;

                try{
                    return new URL(data.toString());
                }catch (MalformedURLException mue){
                    throw new ParseException("Couldn't parse URL", mue);
                }
            case DATETIME:
                if(data instanceof ZonedDateTime)
                    return data;

                if(data instanceof Timestamp)
                    return ((Timestamp)data).toLocalDateTime().atZone(ZoneOffset.UTC);

                return parseDateTime(data);
            case TIME:
                if (data instanceof LocalTime)
                    return data;

                return parseTime(data);
            case IPADDRESS:
                if(data instanceof byte[]){
                    byte[] bytes = (byte[]) data;

                    try {
                        return InetAddress.getByAddress(bytes).getHostAddress();
                    }catch (UnknownHostException e){
                        throw new ParseException("Couldn't parse IP address", e);
                    }
                }

                return data.toString();
            default:
                throw new ParseException("Unsupported type");
        }
    }

    //Private functions below here to keep rest of code tidy

    private static LocalDate parseDate(Object o) throws ParseException{
        String s = o.toString();

        try{
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        }catch (DateTimeParseException e){}

        try{
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("d-MMM-yyyy"));   //6-Nov-2017
        }catch (DateTimeParseException e){}

        throw new ParseException("Couldn't parse date "+s+" - unrecognised format");
    }

    private static LocalTime parseTime(Object o) throws ParseException{
        String s = o.toString();

        try{
            return LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME);
        }catch (DateTimeParseException e){}

        throw new ParseException("Couldn't parse time "+s+" - unrecognised format");
    }

    private static ZonedDateTime parseDateTime(Object o) throws ParseException{
        String s = o.toString();

        try{
            return ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
        }catch (DateTimeParseException e){}

        try{
            return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
        }catch (DateTimeParseException e){}

        try{
            return LocalDateTime.parse(s, DATE_TIME).atZone(ZoneOffset.UTC);
        }catch (DateTimeParseException e){}

        try {
            Long epoch = Long.parseLong(s);
            if(s.length() > 10) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
            }else{
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
            }
        }catch (NumberFormatException e){}

        throw new ParseException("Couldn't parse datetime "+s+" - unrecognised format");
    }
}
