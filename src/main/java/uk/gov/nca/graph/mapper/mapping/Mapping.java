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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.gov.nca.graph.mapper.exceptions.ParseException;

/**
 * Simple object to hold information about a mapping
 */
public class Mapping {
    private DataType dataType;
    private String field;
    private Object literal;

    private static final Pattern VALID_MAPPING = Pattern.compile("_([A-Z]+)\\((.*)\\)");

    /**
     * Create an empty mapping
     */
    public Mapping(){ }

    /**
     * Create a mapping with DataType LITERAL and the provided value
     */
    public Mapping(Object literal){
        this.dataType = DataType.LITERAL;
        this.literal = literal;
    }

    /**
     * Create a mapping with the provided DataType and value
     */
    public Mapping(DataType dataType, String field){
        this.dataType = dataType;

        if(dataType == DataType.LITERAL){
            this.literal = field;
        }else {
            this.field = field;
        }
    }

    public DataType getDataType() {
        return dataType;
    }
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }

    public Object getLiteral() {
        return literal;
    }
    public void setLiteral(Object literal) {
        this.literal = literal;
    }

    public static Mapping fromString(String s) throws ParseException {
        Matcher m = VALID_MAPPING.matcher(s);

        if(!m.matches()){
            throw new ParseException("Mapping is not valid format");
        }

        Mapping mapping = new Mapping();
        try {
            mapping.setDataType(DataType.valueOf(m.group(1)));
        }catch(IllegalArgumentException e){
            throw new ParseException("Unrecognised data type", e);
        }

        if(mapping.getDataType() == DataType.LITERAL){
            mapping.setLiteral(m.group(2));
        }else{
            mapping.setField(m.group(2));
        }

        return mapping;
    }

    @Override
    public boolean equals(Object o){
        if(o == null || !(o instanceof Mapping))
            return false;

        Mapping m = (Mapping) o;

        if(getDataType() != m.getDataType())
            return false;

        return isEqual(getField(), m.getField()) && isEqual(getLiteral(), m.getLiteral());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), getField(), getLiteral());
    }

    @Override
    public String toString() {
        if(getDataType() == DataType.LITERAL){
            return "_LITERAL(" + getLiteral() + ")";
        }else{
            return "_"+getDataType() + "(" + getField() + ")";
        }

    }

    private boolean isEqual(Object o1, Object o2){
        if(o1 == null && o2 == null)
            return true;

        return o1 != null && o1.equals(o2);
    }
}
