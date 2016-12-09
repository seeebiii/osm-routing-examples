package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Reads an OSM file into a {@link Graph} object which contains nodes and edges.
 */
public class SimpleNodeEdgeReader {

    private static final Logger logger = LoggerFactory.getLogger(SimpleNodeEdgeReader.class);

    protected TLongIntMap nodeCounter = new TLongIntHashMap();
    protected TLongIntMap osmIdMapping;
    protected List<Object> ways = new ArrayList<>();
    protected int optimizedWaysCounter = 0;
    protected int edgeCounter = 0;

    protected ReaderElementValidator validator = new ReaderElementValidator();
    protected Graph graph;
    protected File osmFile = null;


    public SimpleNodeEdgeReader(String osmFile) {
        this.osmFile = new File(osmFile);
    }


    public Graph getGraph() {
        if (this.graph == null) {
            throw new IllegalStateException("Can't access the graph if it is not built. First import some data!");
        }
        return this.graph;
    }


    /**
     * Imports the data fromm the given {@link #osmFile} if it exists.
     * First the ways are imported and optimized, i.e. areas are kicked out as well as other unnecessary data.
     * Then nodes and edges are imported and the graph is built up.
     *
     * @return the reader instance (this)
     * @throws Exception in case of IO problems
     */
    public SimpleNodeEdgeReader importData() throws Exception {
        if (this.osmFile != null && this.osmFile.exists()) {
            logger.info("--------------------------- START TO IMPORT DATA -------------");
            long start = System.currentTimeMillis();

            logger.info("--------------------------- READ WAYS ------------------------");
            readWays();
            logger.info("--------------------------- OPTIMIZE WAYS --------------------");
            optimizeWays();
            logger.info("--------------------------- READ NODES -----------------------");
            readNodesOfWays();
            logger.info("--------------------------- SORT NODES -----------------------");
            sortNodes();
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


    protected void readWays() throws Exception {
        processFile("way", item -> {
            switch (item.getType()) {
                case ReaderElement.WAY:
                    ReaderWay way = (ReaderWay) item;
                    final TLongList nodeList = way.getNodes();

                    if (this.validator.isValidWay(way) && nodeList.size() > 1) {
                        processWayNodes(nodeList);
                        this.ways.add(new Way(way, this.validator.isOneWay(way)));
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


    protected void optimizeWays() {
        logger.info("Before optimization: " + this.nodeCounter.size() + " nodes have to be imported.");

        this.ways.forEach(wayObj -> {
            Way way = (Way) wayObj;
            final TLongList oldNodes = way.getNodes();
            final int oldNodesSize = oldNodes.size();

            // eliminate all nodes which are just used for this way so that less data needs to be imported;
            // also count edges which have to be imported so that we can create the graph
            for (int j = 0; j < oldNodesSize; j++) {
                this.edgeCounter += this.validator.isNotOneWay(way) ? 2 : 1;
            }

            this.optimizedWaysCounter++;

            if (this.optimizedWaysCounter % 100000 == 0) {
                logger.debug("Optimized " + this.optimizedWaysCounter + " ways.");
            }
        });

        // initiate garbage collection to avoid memory problems
        System.gc();

        // now create graph, because we know how many nodes and edges we will have
        this.graph = new Graph(this.nodeCounter.size(), this.edgeCounter);
        // initialize the osm mapping map now
        this.osmIdMapping = new TLongIntHashMap(this.nodeCounter.size());

        logger.info("After optimization: " + this.nodeCounter.size() + " nodes have to be imported.");
        logger.info(this.edgeCounter + " edges have to be imported.");
    }


    protected void readNodesOfWays() throws Exception {
        processFile("node", item -> {
            switch (item.getType()) {
                case ReaderElement.NODE:
                    ReaderNode readerNode = (ReaderNode) item;

                    // only import a node if it is used by a way (this was evaluated when the ways have been imported)
                    if (this.nodeCounter.containsKey(readerNode.getId())) {
                        final Node node = new Node(readerNode.getLat(), readerNode.getLon());
                        node.setId(readerNode.getId());
                        int nodeIndex = this.graph.addNode(node);
                        this.osmIdMapping.put(readerNode.getId(), nodeIndex);

                        return true;
                    }
            }

            return false;
        });
    }


    private void sortNodes() {
        Node[] nodes = this.graph.sortNodesAndSetGraphBoundaries().getNodes();
        for (int i = 0; i < nodes.length; i++) {
            this.osmIdMapping.put(nodes[i].getId(), i);
        }
        logger.info("Sorted nodes according to latitude/longitude.");
    }


    private void readEdgesOfWays() {
        this.ways.forEach(wayObj -> {
            Way way = (Way) wayObj;
            final TLongList oldNodes = way.getNodes();
            final int oldNodesSize = oldNodes.size();
            long sourceNode = oldNodes.get(0);

            for (int j = 1; j < oldNodesSize; j++) {
                long nodeId = oldNodes.get(j);
                addEdgeToGraph(way, sourceNode, nodeId);
                sourceNode = nodeId;
            }
        });

        logger.info("Finished import: Imported " + this.edgeCounter + " edges.");
        logger.debug("--- Graph ---\n" + this.graph.toSampleString());
    }


    private void addEdgeToGraph(Way way, long sourceNodeOsmId, long targetNodeOsmId) {
        int sourceNodeIndex = this.osmIdMapping.get(sourceNodeOsmId);
        int targetNodeIndex = this.osmIdMapping.get(targetNodeOsmId);
        this.graph.addEdge(new Edge(sourceNodeIndex, targetNodeIndex));

        if (this.validator.isNotOneWay(way)) {
            // in case the way can be used in both directions, we need to add a reverse edge
            this.graph.addEdge(new Edge(targetNodeIndex, sourceNodeIndex));
        }
    }


    private void sortGraph() {
        // sort edges and connect nodes with edges
        this.graph.sortAndConnectData();

        logger.info("Finished sorting.");
        logger.debug("--- Graph ---\n" + this.graph.toSampleString());
    }


    private void cleanUp() {
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