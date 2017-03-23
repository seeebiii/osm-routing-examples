# osm-routing-examples
A playground containing examples how to read OSM data and calculate routes. Also provides a basic Swing 
application for routing and a website to for routing and finding POIs.

## Author
Sebastian Hesse

## Examples
![Browser view of the routing website](/docu/img/browser_view.jpg "Browser View")

Here are some example classes for different use cases.

**Note:**
I prefer to use the word _node_ instead of _vertex_ in terms of graphs. So don't be irritated.

**Download OSM files:**
You can download OSM files for Germany (and other areas as well) on [this website](http://download.geofabrik.de/).


### PBF-Reader
Example to show how to read nodes and ways of an OSM PBF file.
Uses [Graphhopper](https://github.com/graphhopper/graphhopper) to read an OSM PBF file.
See [PbfReader](/src/main/java/de/sebastianhesse/pbf/reader/PbfReader.java)

### NodeEdgeReader
Example to show how to simply read nodes and ways into a graph of nodes and edges.
Uses [Graphhopper](https://github.com/graphhopper/graphhopper) to read an OSM PBF file.
See [NodeEdgeReader](/src/main/java/de/sebastianhesse/pbf/reader/SimpleNodeEdgeReader.java)

### OsmMapViewer
Example to show how to connect OSM data with a Swing application and also be able to route between two points.
Uses [JMapViewer](http://wiki.openstreetmap.org/wiki/JMapViewer) to start a Swing application and show a map.
See [OsmMapViewer](/src/main/java/de/sebastianhesse/pbf/viewer/OsmMapViewer.java)

### Dropwizard Server
A [Dropwizard](http://www.dropwizard.io/) application setting up a routing API accessible via REST.
See [this package](/src/main/java/de/sebastianhesse/pbf/dropwizard)

In order to start the server, just hand over a configuration file and an OSM file.
The *.yml and *.osm.pbf file must be somewhere on the local system.
Take a look at the [example *.yml file](/src/main/resources/dropwizard.yml).
```
$ mvn clean package
$ java -jar target/osm-routing-examples-1.2.0.jar server /path/to/config.yml /path/to/osm/file
```

#### TMC Support
Optionally you can start the server by providing some TMC data. For this you need the a Location Code List
(e.g. for Germany, you can request one here:
https://www.bast.de/DE/Verkehrstechnik/Fachthemen/v2-LCL/location-code-list_node.html) and an event list
(most likely delivered together with the Location Code List). You also need some TMC data files. Then you can
 start the server with the following arguments:
 
```
$ java -jar target/osm-routing-examples-1.2.0.jar server config.yml osm_data.osm.pbf lcl.csv event_list.csv /path/to/tmc/directory
``` 

A web frontend will be served from:
```
http://localhost:8080/index.html
```


##### REST API
```
### Get a path between two points (lat1,lon1) and (lat2,lon2) ###
vehicle: car, pedestrian
mode: fastest, shortest 
GET /api/route?lat1=...&lon1=...&lat2=...&lon2=...&vehicle=car&mode=fastest


### Get certain POIs around a given position ###
pid: if known, the id of the position/node, otherwise just -1
maxDistance: maximum distance to search for gas stations around the position
typeKey: key of a Nominatim category, e.g. Amenity
typeValue: value of a Nominatim category, e.g. Fuel
GET /api/pois?lat=...&lon=...&pid=...&maxDistance=20&typeKey=...&typeValue=...


### Request all available POI types ###
OPTIONS /api/pois


### Get last updated ways where TMC traffic events have fired for certain hour ###
GET /api/traffic


### Update the routing graph with traffic data for a certain hour ###
PUT /api/traffic/{hour}


### Remove all traffic data from the routing graph ###
DELETE /api/traffic


### Get meta information about the OSM backend ###
GET /api/meta
```

## Development
1. Check out the repository: ``git clone https://github.com/seeebiii/osm-routing-examples``
2. Run ```main``` method from ```DropwizardApplication.java``` with the arguments: ``server /path/to/config.yml /path/to/osm/file``
3. Run [Gulp](http://gulpjs.com/): ``gulp watch``
4. Start coding!


## License
MIT License

Copyright (c) 2017 [Sebastian Hesse](https://www.sebastianhesse.de/)

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
