# Graph Mapper

This tool maps from a structured data source (e.g. CSV or JSON-lines) to a graph format (e.g. GraphML, or directly into a Gremlin database). It is fully configurable, and designed to be schema-agnostic.

## Using the tool

To run the tool, use the following command:

	java -cp mapper-1.0.jar uk.gov.nca.graph.mapper.cli.MapDataToGraph

This will display a help message detailing the different command line options. Some of these command line options are required, and the help message is displayed if these aren't provided.
The full list of options is as follows.

| Short Option | Long Option | Default Value | Required | Description |
| --- | --- | --- | --- | --- |
| c | config | | Yes | The mapping configuration file with which to parse the data. See below for full details. |
| d | data | | Yes | The input data file to process and convert into a graph, or the JDBC connection string if using SQL. |
| f | format | CSV | No | The format that the data file is in. Possible options are CSV, TSV, JSON, JSONL (for JSON-Lines) or SQL (case-insensitive). |
| g | graph | | Yes | The Tinkerpop graph configuration file (follows the standard Tinkerpop format). Examples of this file for GraphML and OrientDB are provided in the `examples/` folder. |
| h | headers | false | No | The CSV/TSV file has a header row as the first row. |
| t | table | | If using SQL format | The SQL table to process |
| u | username | | No | The username for the SQL database (authentication will not be used if this isn't supplied) |
| p | password | | No | The password for the SQL database (authentication will not be used if this isn't supplied) |
| q | query | | No | The SQL query to use to select data (if provided, `table` will be ignored) |
|  | prov | | No | If provided, then the given prov key will be added to every element |

An example full command would therefore be as follows:

	java -cp mapper-1.0.jar uk.gov.nca.graph.mapper.cli.MapDataToGraph -c example/companies/companies.map -d example/companies/companies.jsonl -g example/graphml.properties -f JSONL -prov abc123
	
If you are using the SQL format, you must include the necessary SQL driver on the classpath. For example:

	java -classpath .:mapper-1.0.jar:postgresql-42.1.4.jar uk.gov.nca.graph.mapper.cli.MapDataToGraph -c example/companies/companies.map -d jdbc:postgresql://localhost:5432/example -t companies -g example/graphml.properties -f SQL

## Generating Files

An additional tool is provided to create sample graphs using a mapping file, for testing purposes.

To run the tool, use the following command:

	java -cp mapper-1.0.jar uk.gov.nca.graph.mapper.cli.GenerateGraphFromMap
	
This will display a help message detailing the available options.

## Mapping Configuration

The mapping configuration file describes how the structured data should be transformed into a graph.
It is flexible enough to describe most graph schemas, although support for complex operations such as manipulating data prior to mapping it into the graph is still limited.
The configuration file is a YAML file, and standard YAML formatting rules apply.

The configuration file has two required top level objects, `vertices` and `edges`.
These objects consist of a list of sub-objects, with each sub-object describing either a vertex (node) or an edge (link) respectively.

A third top level object, `filters`, is optional and can be used to filter the data prior to converting it to a graph.

You can also specify `lenient: true` to make the parsing of fields more lenient. If `true`, then rather than skipping any un-parseable data fields, their String value will be used instead.

When processing the data, each row in the data file will be processed separately and the mapping configuration will be used to map that row of data to a sub-graph and insert it into the main graph via the Gremlin interface.

### Vertex Mappings

Each sub-object under the `vertices` object represents a single vertex in the output graph. Some properties are 'reserved', and used by the tool. Any other property will be added to the vertex. The reserved properties are:

* `_except` allows you to skip a particular vertex for a data row. The value should be a map of field names to value, where if any of the values map then the vertex is skipped.
* `_id` sets an internal ID for the vertex, which is used when creating edges between vertices. The value can be anything, and is not copied across onto the graph.
* `_merge` - if `true`, then the vertex will be merged with the first existing vertex in the graph for which all the properties on the new vertex exist and have the same value (additional properties on the existing vertex are ignored). If no matching vertex is found, or `false`, then a new vertex will be created. Note that all vertices are merged on the `identifier` property regardless of this setting.
* `_type` sets the type or label of the vertex

The value of each property can be one of the following to read data from the data file.

* `_BOOLEAN(*field name*)`
* `_DATETIME(*field name*)`
* `_DATE(*field name*)`
* `_DOUBLE(*field name*)`
* `_INTEGER(*field name*)`
* `_IPADDRESS(*field name*)`
* `_STRING(*field name*)`
* `_TIME(*field name*)`
* `_URL(*field name*)`
* `_LITERAL(*literal value*)`

The field name is either the JSON field name (or CSV/TSV column header if `-h` was specified), or the one-based column number for CSV/TSV files.
If the field can't be found, or the value can't be parsed to the correct type, then the property will be skipped for that data row.
Any value not in the format above will be interpretted as a literal value.

If a list of values is provided, then the parsed outputs of each of these will be concatenated into a single string. However, the list can be case to a specific type by including an empty type as the first item in the list. For instance, the following would create a URL rather than a String:

    url:
      - _URL()
      - _LITERAL(http://www.example.com/users/)
      - _INTEGER(user_id)

### Edge Mappings

Edges are simpler than vertices, and only support three properties: `_type`, `_src`, and `_tgt`.

* `_type` sets the type or label of the edge.
* `_src` sets the source vertex of the edge, and should be the `_id` of the vertex.
* `_tgt` sets the target vertex of the edge, and should be the `_id` of the vertex.

Any other properties are ignored - they are not added to the edge.
As with vertices, property values can be pulled from the data using the same format.

### Filters

Filters can be used to filter the data before processing. The optional `filters` object is used to configure this filtering.

The `filters` object has one reserved property, `_exists`, which takes a list of fields.
If these fields don't exist or they are empty, then the data row will be skipped.

Any other properties listed must be present with the specified value for the data row to be processed.

### Annotated Example

The following example (taken from the unit tests) demonstrates all of the above features. It has been annotated to explain what each line is doing:

	filters:			# Create a filters object to filter our data
	  _exists: name			# Only accept data rows that have a non-empty name property
	  surname:			# Only accept data rows that have a surname of Jones or Smith
	  - Jones
	  - Smith

	vertices:			# Create a vertices object to configure our vertex mapping
	- _id: 1			# Create a vertex with internal ID 1
	  _type: Person			# Set the type to Person
	  name: _STRING(1)		# Read the name from the first column in the data as a String
	  dateOfBirth: _DATE(3)		# Read the date of birth from the third column in the data and parse it to a Date
	  gender: _STRING(2)		# Read the gender from the second column in the data as a String
	  source: example.txt		# Set the source property to "example.txt"
	- _id: 2			# Create a vertex with internal ID 2
	  _type: Person			# Set the type to Person
	  _merge: true			# Merge this vertex with any Person entity that has the same name
	  _except:
	    3: Sam			# If the third column has the value "Sam", then it will be skipped and this vertex (and any edges linking it) won't be created
	  name:				# Set the name property to be the concatenated value of the fourth and fifth columns
	  - _STRING(4)
	  - " "
	  - _STRING(5)

	edges:				# Create an edges object to configure our edge mapping
	- _type: parentOf		# Set the type to parentOf
	  _src: 1			# Link from the node with internal ID 1...
	  _tgt: 2			# ...to the node with internal ID 2

If we were to feed in the following CSV data

	name,gender,dob,firstName,surname
	Amy,female,1980-02-12,Paul,Jones
	Peter,male,1982-11-30,Paul,Jones
	Mary,female,1981-04-12,Bob,Edwards
	,male,1981-05-01,Bob,Jones

Then we would end up with a graph that looks like the following (note that the third and fourth rows have been filtered out):

	(name: Amy, gender: female, dateOfBirth: 1980-02-12, source: example.txt) -- parentOf --> (name: Paul Jones) <-- parentOf -- (name: Peter, gender: male, dateOfBirth: 1982-11-30, source: example.txt)



