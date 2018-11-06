# Graph Mapper Examples

Below are a couple of examples of processing data files and producing GraphML.
To run them, you can use the following command (using the `addresses` example):

    java -cp mapper-1.0.jar uk.gov.nca.graph.mapper.cli.MapDataToGraph -c addresses/addresses.map -d addresses/addresses.csv -g graphml.properties

This will produce an `output.graphml` file (or add to `output.graphml` if it already exists), containing the graph.
GraphML files can be viewed in tools such as Gephi or Cytoscape.