package de.sebastianhesse.pbf.reader;

import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.list.TLongList;
import gnu.trove.map.hash.TLongIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads an OSM file into a {@link Graph} object which contains nodes and edges. Does no optimization,
 * just simple reading.
 */
public class SimpleNodeEdgeReader extends AbstractNodeEdgeReader {

    private static final Logger logger = LoggerFactory.getLogger(SimpleNodeEdgeReader.class);

    protected int wayCounter = 0;
    protected int edgeCounter = 0;


    public SimpleNodeEdgeReader(String osmFile) {
        super(osmFile);
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

            this.wayCounter++;

            if (this.wayCounter % 100000 == 0) {
                logger.debug("Optimized " + this.wayCounter + " ways.");
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


    protected void readEdgesOfWays() {
        this.ways.forEach(wayObj -> {
            Way way = (Way) wayObj;
            final TLongList oldNodes = way.getNodes();
            final int oldNodesSize = oldNodes.size();

            long[] crossings = new long[oldNodesSize];
            for (int i = 0; i < oldNodesSize; i++) {
                Node node = this.graph.getNodes()[this.osmIdMapping.get(oldNodes.get(i))];
                if (node.isCrossing()) {
                    crossings[i] = oldNodes.get(i);
                } else {
                    crossings[i] = -1;
                }
            }

            long sourceNode = oldNodes.get(0);
            long lastCrossing;
            long nextCrossing;

            for (int j = 1; j < oldNodesSize; j++) {
                long nodeId = oldNodes.get(j);
                lastCrossing = getLastCrossing(crossings, j);
                nextCrossing = getNextCrossing(crossings, j);
                addEdgeToGraph(way, sourceNode, nodeId, lastCrossing, nextCrossing);
                sourceNode = nodeId;
            }
        });

        logger.info("Finished import: Imported " + this.edgeCounter + " edges.");
        logger.debug("--- Graph ---\n" + this.graph.toSampleString());
    }


    private long getLastCrossing(long[] crossings, int idx) {
        for (int i = idx - 1; i >= 0; i--) {
            if (crossings[i] > -1) {
                return crossings[i];
            }
        }
        return -1;
    }


    private long getNextCrossing(long[] crossings, int idx) {
        for (int i = idx; i < crossings.length; i++) {
            if (crossings[i] > -1) {
                return crossings[i];
            }
        }
        return -1;
    }
}