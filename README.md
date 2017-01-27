# osm-routing-examples
A playground containing examples how to read OSM data and calculate routes. Also provides a basic Swing 
application for routing and a website to for routing and finding POIs.

## Author
Sebastian Hesse

## Examples
![Browser view of the routing website](/docu/img/browser_view.jpg "Browser View")

Here are some example classes for different use cases.

#### Note
I prefer to use the word _node_ instead of _vertex_ in terms of graphs. So don't be irritated.

### PBF-Reader
Example to show how to read nodes and ways of an OSM PBF file.
See [PbfReader](/src/main/java/de/sebastianhesse/pbf/reader/PbfReader.java)

### NodeEdgeReader
Example to show how to simply read nodes and ways into a graph of nodes and edges.
See [NodeEdgeReader](/src/main/java/de/sebastianhesse/pbf/reader/SimpleNodeEdgeReader.java)

### OsmMapViewer
Example to show how to connect OSM data with a Swing application and also be able to route between two points.
See [OsmMapViewer](/src/main/java/de/sebastianhesse/pbf/viewer/OsmMapViewer.java)

### Dropwizard Server
A Dropwizard application setting up a routing API accessible via REST.
See [DropwizardPackage](/src/main/java/de/sebastianhesse/pbf/dropwizard)

In order to start the server:
```
$ mvn clean package
$ java -jar target/osm-routing-examples.jar server /path/to/config.yml /path/to/osm/file
```

A web frontend will be served from:
```
http://localhost:8080/index.html
```

Access the REST API with:
```
### Get a path between two points (lat1,lon1) and (lat2,lon2) ###
vehicle: car, pedestrian
mode: fastest, shortest 
GET /api/route?lat1=...&lon1=...&lat2=...&lon2=...&vehicle=car&mode=fastest


### Get all gas stations around a given position ###
maxDistance: maximum distance to search for gas stations around the position
GET /api/gasstations?lat=...&lon=...&maxDistance=20
```

## License
MIT License

Copyright (c) 2017 Sebastian Hesse

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
