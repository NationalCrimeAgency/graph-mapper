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

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;
import uk.gov.nca.graph.utils.ElementUtils;
import uk.gov.nca.graph.mapper.mapping.DataType;
import uk.gov.nca.graph.mapper.mapping.EdgeMap;
import uk.gov.nca.graph.mapper.mapping.Mapping;
import uk.gov.nca.graph.mapper.mapping.VertexMap;

public class GrapherTest {

    @Test
    public void testIgnoreEmpty() throws Exception{
        Graph graph = TinkerGraph.open();

        Configuration conf = new Configuration();

        VertexMap vertexMap = new VertexMap();
        vertexMap.setType("Person");
        vertexMap.setId("person");
        vertexMap.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vertexMap.setProperty("age", Arrays.asList(new Mapping(DataType.INTEGER, "age")));
        vertexMap.setProperty("alias", Arrays.asList(new Mapping(DataType.STRING, "alias")));
        vertexMap.setProperty("sameAs", Arrays.asList(new Mapping("http://www.example.com/"), new Mapping(
            DataType.STRING, "username")));
        vertexMap.setProperty("website", Arrays.asList(new Mapping("http://www.example.com/"), new Mapping(
            DataType.STRING, "webpage")));
        vertexMap.setProperty("hasChildren", Arrays.asList(new Mapping(DataType.BOOLEAN, "parent")));
        vertexMap.setProperty("placeOfBirth", Arrays.asList(new Mapping("Edinburgh"), new Mapping(", "), new Mapping("Scotland")));

        conf.getVertices().add(vertexMap);

        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bob");
        data.put("age", 29);
        data.put("alias", "Unknown");
        data.put("username", "bsmith");

        grapher.addDataToGraph(data, graph);

        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Vertex v = vertices.get(0);
        assertEquals("Bob", ElementUtils.getProperty(v, "name"));
        assertEquals(29, ElementUtils.getProperty(v, "age"));
        assertEquals("Unknown", ElementUtils.getProperty(v, "alias"));
        assertEquals("http://www.example.com/bsmith", ElementUtils.getProperty(v, "sameAs"));
        assertNull(ElementUtils.getProperty(v, "website"));
        assertNull(ElementUtils.getProperty(v, "hasChildren"));
        assertEquals("Edinburgh, Scotland", ElementUtils.getProperty(v, "placeOfBirth"));
    }

    @Test
    public void testExcept() throws Exception{
        Graph graph = TinkerGraph.open();

        Configuration conf = new Configuration();

        VertexMap vertexMap = new VertexMap();
        vertexMap.setType("Person");
        vertexMap.setId("person");
        vertexMap.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vertexMap.setProperty("age", Arrays.asList(new Mapping(DataType.INTEGER, "age")));
        vertexMap.setProperty("alias", Arrays.asList(new Mapping(DataType.STRING, "alias")));

        Map<String, Mapping> exceptMap = new HashMap<>();
        exceptMap.put("age", new Mapping(0));
        exceptMap.put("alias", new Mapping(DataType.STRING, "name"));
        vertexMap.setExcept(exceptMap);

        conf.getVertices().add(vertexMap);

        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bob");
        data.put("age", 29);
        data.put("alias", "Unknown");

        grapher.addDataToGraph(data, graph);

        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Bobbi");
        data2.put("age", 0);

        grapher.addDataToGraph(data2, graph);

        vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Map<String, Object> data3 = new HashMap<>();
        data3.put("name", "Bob");
        data3.put("age", 0);
        data3.put("alias", "Bob");

        grapher.addDataToGraph(data3, graph);

        vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        graph.close();
    }

    @Test
    public void testMerge() throws Exception{
        Graph graph = TinkerGraph.open();

        Configuration conf = new Configuration();

        VertexMap v1 = new VertexMap();
        v1.setType("Person");
        v1.setId("person1");
        v1.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        v1.setProperty("identifier", Arrays.asList(new Mapping(
            DataType.LITERAL, "user."), new Mapping(DataType.INTEGER, "id")));

        VertexMap v2 = new VertexMap();
        v2.setType("Person");
        v2.setId("person2");
        v2.setMerge(true);
        v2.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name2")));

        conf.getVertices().add(v1);
        conf.getVertices().add(v2);

        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Rebecca");
        data.put("id", 1);
        data.put("name2", "Rebecca");

        grapher.addDataToGraph(data, graph);

        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Vertex v = vertices.get(0);
        assertEquals("Rebecca", v.property("name").value());
        assertEquals("user.1", v.property("identifier").value());

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Josephine");
        data2.put("id", 2);
        data2.put("name2", "Rebecca");

        grapher.addDataToGraph(data2, graph);

        vertices = graph.traversal().V().toList();
        assertEquals(2, vertices.size());

        graph.close();
    }

    @Test
    public void testMergeWithExistingOnIdentifier() throws Exception{
        Graph graph = TinkerGraph.open();

        //Add an existing node
        graph.addVertex(T.label, "Person", "identifier", "person.123", "gender", "Female");

        //Configure a map that adds one person
        Configuration conf = new Configuration();

        VertexMap vm = new VertexMap();
        vm.setType("Person");
        vm.setId("person1");
        vm.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vm.setProperty("identifier", Arrays.asList(new Mapping(
            DataType.LITERAL, "person."), new Mapping(DataType.INTEGER, "id")));

        conf.getVertices().add(vm);

        //Add new data to graph, should merge with existing node as the identifier will be the same
        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Anna");
        data.put("id", 123);

        grapher.addDataToGraph(data, graph);

        //Check that we still have one node, but with both sets of data
        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Vertex v = vertices.get(0);
        assertEquals("person.123", v.property("identifier").value());
        assertEquals("Anna", v.property("name").value());
        assertEquals("Female", v.property("gender").value());

        graph.close();
    }

    @Test
    public void testMergeOnDottedProperty() throws Exception{
        Graph graph = TinkerGraph.open();

        //Add an existing node
        graph.addVertex(T.label, "Object", "object.name", "box", "object.colour", "red");

        //Configure a map that adds one object
        Configuration conf = new Configuration();

        VertexMap vm = new VertexMap();
        vm.setType("Object");
        vm.setId("obj1");
        vm.setProperty("object.name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vm.setMerge(true);

        conf.getVertices().add(vm);

        //Add new data to graph, should merge with existing node as the properties will be the same
        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "box");

        grapher.addDataToGraph(data, graph);

        //Check that we still have one node, but with both sets of data
        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Vertex v = vertices.get(0);
        assertEquals("box", v.property("object.name").value());
        assertEquals("red", v.property("object.colour").value());

        graph.close();
    }

    @Test
    public void testNested() throws Exception{
        Graph graph = TinkerGraph.open();

        //Configure a map that adds one person
        Configuration conf = new Configuration();

        VertexMap vm = new VertexMap();
        vm.setType("Person");
        vm.setId("person1");
        vm.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vm.setProperty("age", Arrays.asList(new Mapping(DataType.INTEGER, "details.age")));
        vm.setProperty("gender", Arrays.asList(new Mapping(DataType.STRING, "details.gender")));

        conf.getVertices().add(vm);

        //Add new data to graph, should merge with existing node as the properties will be the same
        Grapher grapher = new Grapher(conf);

        Map<String, Object> details = new HashMap<>();
        details.put("age", 30);
        details.put("gender", "male");

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Nathan");
        data.put("details", details);

        grapher.addDataToGraph(data, graph, true);

        //Check that we still have one node, but with both sets of data
        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Vertex v = vertices.get(0);
        assertEquals("Nathan", v.property("name").value());
        assertEquals(30, v.property("age").value());
        assertEquals("male", v.property("gender").value());

        graph.close();
    }

    @Test
    public void testAudit() throws Exception{
        Graph graph = TinkerGraph.open();

        Configuration conf = new Configuration();

        VertexMap vertexMap1 = new VertexMap();
        vertexMap1.setType("Person");
        vertexMap1.setId("person");
        vertexMap1.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));
        vertexMap1.setProperty("age", Arrays.asList(new Mapping(DataType.INTEGER, "age")));

        VertexMap vertexMap2 = new VertexMap();
        vertexMap2.setType("Email");
        vertexMap2.setId("email");
        vertexMap2.setProperty("identifier", Arrays.asList(new Mapping(DataType.STRING, "email")));

        EdgeMap edgeMap = new EdgeMap();
        edgeMap.setType("email");
        edgeMap.setSourceId("person");
        edgeMap.setTargetId("email");

        conf.getVertices().add(vertexMap1);
        conf.getVertices().add(vertexMap2);
        conf.getEdges().add(edgeMap);

        Grapher grapher = new Grapher(conf);

        Map<String, Object> audit = new HashMap<>();
        audit.put("prov", "abc123");

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bob");
        data.put("age", 29);
        data.put("email", "bob@example.com");

        grapher.addDataToGraph(data, graph, audit);

        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(2, vertices.size());

        Vertex v1 = vertices.get(0);
        assertEquals("Person", v1.label());
        assertEquals("Bob", ElementUtils.getProperty(v1, "name"));
        assertEquals(29, ElementUtils.getProperty(v1, "age"));
        assertEquals("abc123", ElementUtils.getProperty(v1, "prov"));

        Vertex v2 = vertices.get(1);
        assertEquals("Email", v2.label());
        assertEquals("bob@example.com", ElementUtils.getProperty(v2, "identifier"));
        assertEquals("abc123", ElementUtils.getProperty(v2, "prov"));

        List<Edge> edges = graph.traversal().E().toList();
        assertEquals(1, edges.size());

        Edge e = edges.get(0);
        assertEquals("email", e.label());
        assertEquals("abc123", ElementUtils.getProperty(e, "prov"));
    }

    @Test
    public void testExceptNull() throws Exception{
        Graph graph = TinkerGraph.open();

        Configuration conf = new Configuration();

        VertexMap vertexMap = new VertexMap();
        vertexMap.setType("Person");
        vertexMap.setId("person");
        vertexMap.setProperty("name", Arrays.asList(new Mapping(DataType.STRING, "name")));

        Map<String, Mapping> exceptMap = new HashMap<>();
        exceptMap.put("name", null);
        vertexMap.setExcept(exceptMap);

        conf.getVertices().add(vertexMap);

        Grapher grapher = new Grapher(conf);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bob");

        grapher.addDataToGraph(data, graph);

        List<Vertex> vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", null);

        grapher.addDataToGraph(data2, graph);

        vertices = graph.traversal().V().toList();
        assertEquals(1, vertices.size());

        graph.close();
    }

    @Test
    public void testFlattenMap(){
        Map<String, Object> nested1 = new HashMap<>();
        nested1.put("a", 1);
        nested1.put("b", 2);

        Map<String, Object> nested2 = new HashMap<>();
        nested2.put("a", 3);
        nested2.put("b", 4);
        nested2.put("c", nested1);

        Map<String, Object> nested3 = new HashMap<>();
        nested3.put("a", 5);
        nested3.put("b", 6);
        nested3.put("c", nested2);

        /*
        We want the following structure:
        nested3 = {
            a: 5,
            b: 6,
            c.a: 3,
            c.b: 4,
            c.c.a: 1,
            c.c.b: 2
        }
         */

        Map<String, Object> flat = Grapher.flattenMap(nested3);

        assertEquals(6, flat.size());
        assertEquals(1, flat.get("c.c.a"));
        assertEquals(2, flat.get("c.c.b"));
        assertEquals(3, flat.get("c.a"));
        assertEquals(4, flat.get("c.b"));
        assertEquals(5, flat.get("a"));
        assertEquals(6, flat.get("b"));
    }

    @Test
    public void testFlattenMapException(){
        Map<Integer, Object> nested1 = new HashMap<>();
        nested1.put(1, 1);
        nested1.put(2, 2);

        Map<String, Object> nested2 = new HashMap<>();
        nested2.put("a", 3);
        nested2.put("b", 4);
        nested2.put("c", nested1);

        /*
        We expect the following structure:
        nested2 = {
            a: 3,
            b: 4,
            c: {
                1: 1
                2: 2
            }
        }
         */

        Map<String, Object> flat = Grapher.flattenMap(nested2);

        assertEquals(3, flat.size());
        assertEquals(nested1, flat.get("c"));
        assertEquals(3, flat.get("a"));
        assertEquals(4, flat.get("b"));
    }
}
