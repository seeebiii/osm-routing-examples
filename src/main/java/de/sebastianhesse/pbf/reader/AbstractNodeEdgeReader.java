package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import de.sebastianhesse.pbf.reader.Way.WayBuilder;
import de.sebastianhesse.pbf.reader.traffic.EventListImporter;
import de.sebastianhesse.pbf.reader.traffic.LocationListImporter;
import de.sebastianhesse.pbf.reader.traffic.TmcEventMessagesImporter;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.storage.traffic.EventList;
import de.sebastianhesse.pbf.storage.traffic.LocationList;
import de.sebastianhesse.pbf.storage.traffic.TmcMessageMap;
import de.sebastianhesse.pbf.storage.traffic.TrafficHandler;
import de.sebastianhesse.pbf.util.GraphUtil;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Abstract class to read an OSM file into a {@link Graph} object which contains nodes and edges.
 */
public abstract class AbstractNodeEdgeReader implements NodeEdgeReader {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNodeEdgeReader.class);

    protected TLongIntMap nodeCounter = new TLongIntHashMap();
    protected TLongIntMap osmIdMapping;
    protected List<Object> ways = new ArrayList<>();
    protected ReaderWayValidator validator = new ReaderWayValidator();
    protected Graph graph;
    protected TrafficHandler trafficHandler;
    protected File osmFile = null;
    protected String locationListPath = null;
    protected String eventListPath = null;
    protected String tmcDataDirectory = null;
    protected Map<String, Set<String>> poiTypes;


    public AbstractNodeEdgeReader(String osmFile, String locationList, String eventList, String tmcDataDirectory) {
        this(osmFile);
        this.locationListPath = locationList;
        this.eventListPath = eventList;
        this.tmcDataDirectory = tmcDataDirectory;
    }


    public AbstractNodeEdgeReader(String osmFile) {
        this.osmFile = new File(osmFile);
        if (!this.osmFile.exists()) {
            throw new IllegalArgumentException("You must provide an existing file containing OSM data.");
        }
    }


    public Graph getGraph() {
        if (this.graph == null) {
            throw new IllegalStateException("Can't access the graph if it is not built. First import some data!");
        }
        return this.graph;
    }


    public TrafficHandler getTrafficHandler() {
        if (this.trafficHandler == null) {
            throw new IllegalStateException("Can't access the traffic map if it is not built. First import some data!");
        }
        return this.trafficHandler;
    }


    /**
     * Imports the data fromm the given {@link #osmFile} if it exists.
     * First the ways are imported and optimized, i.e. areas are kicked out as well as other unnecessary data.
     * Then nodes and edges are imported and the graph is built up.
     *
     * @return the reader instance (this)
     * @throws Exception in case of IO problems
     */
    public NodeEdgeReader importData() throws Exception {
        if (this.osmFile != null && this.osmFile.exists()) {
            logger.info("--------------------------- PREPARE IMPORT -------------------");
            prepareImport();
            logger.info("--------------------------- PREPARE TRAFFIC HANDLING ---------");
            importTrafficData();
            logger.info("--------------------------- START TO IMPORT OSM DATA ---------");
            long start = System.currentTimeMillis();

            logger.info("--------------------------- READ WAYS ------------------------");
            readWays();
            logger.info("--------------------------- OPTIMIZE WAYS --------------------");
            optimizeWays();
            logger.info("--------------------------- READ NODES -----------------------");
            readNodesOfWays();
            logger.info("--------------------------- SORT NODES -----------------------");
            sortNodes();
            logger.info("--------------------------- READ POIS ------------------------");
            readPois();
            logger.info("--------------------------- READ EDGES -----------------------");
            readEdgesOfWays();
            logger.info("--------------------------- SORT GRAPH -----------------------");
            sortGraph();
            logger.info("--------------------------- CLEAN UP MEMORY ------------------");
            cleanUp();

            logger.info("--------------------------- FINISHED IMPORT ------------------");
            logger.info("It took " + (System.currentTimeMillis() - start) / 1000 + " seconds to import the data.");
        } else {
            logger.error("Could not start the task, because the OSM file does not exist.");
        }

        return this;
    }


    private void importTrafficData() {
        TmcEventMessagesImporter tmcEventMessagesImporter = new TmcEventMessagesImporter(this.tmcDataDirectory);
        TmcMessageMap tmcMessageMap = new TmcMessageMap(tmcEventMessagesImporter);

        LocationListImporter locationListImporter = new LocationListImporter(this.locationListPath);
        LocationList locationList = locationListImporter.importLocationList();
        logger.debug("Finished import: imported {} entries for location list.", locationList.size());

        EventListImporter eventListImporter = new EventListImporter(this.eventListPath);
        EventList eventList = eventListImporter.importEventList();
        logger.debug("Finished import: imported {} entries for event list.", eventList.size());

        this.trafficHandler = new TrafficHandler(locationList, eventList, tmcMessageMap);

        logger.info("Finished preparation: imported TMC event and location data.");
    }


    private void prepareImport() {
        // load POI type file from classpath
        InputStream inputStream = this.getClass().getResourceAsStream("/poi_nominatim.json");
        if (inputStream == null) {
            // if input stream is null, we must parse the nominatim file first
            inputStream = this.getClass().getResourceAsStream("/poi_nominatim.txt");
            NominatimSpecialPhrasesConverter converter = new NominatimSpecialPhrasesConverter(inputStream);
            this.poiTypes = converter.convertFromStream();
        } else {
            // a json file already exists and we can directly read it without parsing
            NominatimSpecialPhrasesConverter converter = new NominatimSpecialPhrasesConverter(inputStream);
            this.poiTypes = converter.convertFromJsonStream();
        }
        logger.info("Identified {} POI types which will be available later.", this.poiTypes.size());
    }


    protected void readWays() throws Exception {
        processFile("way", item -> {
            switch (item.getType()) {
                case ReaderElement.WAY:
                    ReaderWay readerWay = (ReaderWay) item;
                    final TLongList nodeList = readerWay.getNodes();

                    if (this.validator.isValidWay(readerWay) && nodeList.size() > 1) {
                        processWayNodes(nodeList);
                        Way way = new WayBuilder().setOriginalWay(readerWay).build();
                        this.ways.add(way);
                        return true;
                    }
            }

            return false;
        });
    }


    protected void processWayNodes(TLongList nodeList) {
        int size = nodeList.size();
        for (int i = 0; i < size; i++) {
            final long id = nodeList.get(i);
            if (this.nodeCounter.containsKey(id)) {
                this.nodeCounter.put(id, this.nodeCounter.get(id) + 1);
            } else {
                this.nodeCounter.put(id, 1);
            }
        }
    }


    protected abstract void optimizeWays();


    protected void readNodesOfWays() throws Exception {
        processFile("node", item -> {
            switch (item.getType()) {
                case ReaderElement.NODE:
                    ReaderNode readerNode = (ReaderNode) item;

                    // only import a node if it is used by a way (this was evaluated when the ways have been imported)
                    if (this.nodeCounter.containsKey(readerNode.getId())) {
                        final Node node = new Node(readerNode.getLat(), readerNode.getLon());
                        node.setId(readerNode.getId());
                        node.setCrossing(this.nodeCounter.get(readerNode.getId()) > 1);
                        int nodeIndex = this.graph.addNode(node);
                        this.osmIdMapping.put(readerNode.getId(), nodeIndex);

                        return true;
                    }
            }

            return false;
        });
    }


    protected void sortNodes() {
        Node[] nodes = this.graph.sortNodesAndSetGraphBoundaries().getNodes();
        for (int i = 0; i < nodes.length; i++) {
            this.osmIdMapping.put(nodes[i].getId(), i);
        }
        logger.info("Sorted nodes according to latitude/longitude.");
    }


    protected void readPois() throws Exception {
        processFile("pois", item -> {
            switch (item.getType()) {
                case ReaderElement.NODE:
                    ReaderNode readerNode = (ReaderNode) item;
                    if (!readerNode.hasTags()) {
                        // if the node has no tags, it doesn't make sense to investigate it
                        return false;
                    }

                    // check if node is a POI by verifying the POI key and value
                    Set<String> keys = this.poiTypes.keySet();
                    for (String key : keys) {
                        if (readerNode.hasTag(key, this.poiTypes.get(key))) {
                            // node is a POI, thus add to graph
                            Node node = new Node(readerNode.getLat(), readerNode.getLon());
                            node.setPoi(true);
                            node.setType(key, readerNode.getTag(key));
                            if (this.osmIdMapping.containsKey(readerNode.getId())) {
                                this.graph.addPoi(node, this.osmIdMapping.get(readerNode.getId()));
                            } else {
                                this.graph.addPoi(node);
                            }

                            return true;
                        }
                    }
            }

            return false;
        });

        this.graph.setPoiTypes(this.poiTypes);
    }


    protected abstract void readEdgesOfWays();


    protected void addEdgeToGraph(Way way, long sourceNodeOsmId, long targetNodeOsmId,
                                  long lastCrossing, long nextCrossing) {
        int sourceNodeIndex = this.osmIdMapping.get(sourceNodeOsmId);
        int targetNodeIndex = this.osmIdMapping.get(targetNodeOsmId);
        this.graph.addEdge(getEdge(way, sourceNodeIndex, targetNodeIndex, nextCrossing));

        if (this.validator.isNotOneWay(way)) {
            // in case the way can be used in both directions, we need to add a reverse edge
            this.graph.addEdge(getEdge(way, targetNodeIndex, sourceNodeIndex, lastCrossing));
        }
    }


    protected Edge getEdge(Way way, int sourceNodeIndex, int targetNodeIndex, long crossingId) {
        Edge edge = new Edge(way.getType(), sourceNodeIndex, targetNodeIndex);

        Node source = this.graph.getNodes()[sourceNodeIndex];
        Node target = this.graph.getNodes()[targetNodeIndex];
        edge.setDistance(GraphUtil.getDistance(source, target));
        edge.setSpeed(way.getMaxSpeed());
        edge.setAccess(way.getAccess());
        if (crossingId > -1 && this.osmIdMapping.containsKey(crossingId)) {
            edge.setNextCrossing(this.osmIdMapping.get(crossingId));
        }

        return edge;
    }


    protected void sortGraph() {
        // sort edges and connect nodes with edges
        this.graph.sortAndConnectData();

        logger.info("Finished sorting.");
        logger.debug("--- Graph ---\n" + this.graph.toSampleString());
    }


    protected void cleanUp() {
        // clean up this temporary object to save memory space
        this.nodeCounter.clear();
        this.osmIdMapping.clear();
        this.ways.clear();

        logger.info("Finished cleaning up memory.");
    }


    protected void processFile(String type, ItemHandler itemHandler) throws Exception {
        final OSMInputFile osmInputFile = new OSMInputFile(this.osmFile).setWorkerThreads(2).open();
        int counter = 0;
        ReaderElement item = null;
        while ((item = osmInputFile.getNext()) != null) {
            boolean handled = itemHandler.handle(item);

            if (handled && ++counter % 100000 == 0) {
                logger.debug("Imported " + counter + " objects of type " + type);
            }
        }

        logger.info("Finished import: Imported " + counter + " objects of type " + type);
    }
}