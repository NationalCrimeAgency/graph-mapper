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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads an XML file into a map, looking for a specified element as the top level item.
 *
 * Attributes will be prefixed with a # (e.g. #id)
 *
 * Note that for more complex XML files, this data source is not appropriate as some information
 * may be lost (specifically association information between repeated nested nodes).
 */
public class XmlDataSource implements DataSource {

  private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
  private XMLEventReader eventReader;

  private final String elementName;

  private Map<String, Object> nextObject = null;

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDataSource.class);

  public XmlDataSource(String file, String elementName) throws IOException, XMLStreamException {
    this(new FileInputStream(file), elementName);
  }

  public XmlDataSource(InputStream xmlInputStream, String elementName) throws XMLStreamException {
    eventReader = inputFactory.createXMLEventReader(xmlInputStream);
    this.elementName = elementName;
  }

  @Override
  public void close() throws Exception {
    eventReader.close();
    eventReader = null;
    inputFactory = null;
  }

  @Override
  public boolean hasNext() {
    if(nextObject != null)
      return true;

    //Else, read in next object if possible
    try {
      Map<String, Object> map = null;

      List<String> namespace = new ArrayList<>();

      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();

        if(event.isStartElement()) {
          StartElement startElement = event.asStartElement();

          if(elementName.equals(startElement.getName().getLocalPart())) {
            map = new HashMap<>();

            Iterator<Attribute> attributes = startElement.getAttributes();
            while (attributes.hasNext()) {
              Attribute attribute = attributes.next();
              addToMap(map, namespace, attribute.getValue(), attribute.getName().getLocalPart());
            }
          }else if(map != null) {
            namespace.add(startElement.getName().getLocalPart());

            Iterator<Attribute> attributes = startElement.getAttributes();
            while (attributes.hasNext()) {
              Attribute attribute = attributes.next();
              addToMap(map, namespace, attribute.getValue(), attribute.getName().getLocalPart());
            }

            event = eventReader.nextEvent();
            String content = event.asCharacters().getData();
            if(!content.trim().isEmpty())
              addToMap(map, namespace, content);
          }
        }

        if(event.isEndElement()) {
          EndElement endElement = event.asEndElement();
          if(!namespace.isEmpty())
            namespace.remove(namespace.size() - 1);

          if(elementName.equals(endElement.getName().getLocalPart())) {
            //Finished the current object, so save it and then stop parsing for this pass
            nextObject = map;
            break;
          }
        }
      }
    }catch (XMLStreamException e) {
      LOGGER.error("Unable to read XML Stream", e);
      return false;
    }

    return nextObject != null;
  }

  @Override
  public Map<String, Object> next() {
    if(nextObject == null)
      throw new NoSuchElementException();

    Map<String, Object> ret = nextObject;
    nextObject = null;
    return ret;
  }

  private void addToMap(Map<String, Object> map, List<String> namespace, Object value){
    addToMap(map, namespace, value, null);
  }

  private void addToMap(Map<String, Object> map, List<String> namespace, Object value, String attribute){
    String name = namespace.stream().collect(Collectors.joining("."));
    if(attribute != null)
      name += "#" + attribute;

    Object o = map.get(name);
    if(o == null){
      map.put(name, value);
    }else{
      List<Object> l = new ArrayList<>();
      if(o instanceof Collection) {
        l.addAll((Collection<?>) o);
      }else{
        l.add(o);
      }
      l.add(value);
      map.put(name, l);
    }
  }
}
