package de.sebastianhesse.pbf.reader;

import de.sebastianhesse.pbf.storage.Graph;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads an OSM file into a {@link Graph} object which contains nodes and edges.
 * Optimizes ways by removing all nodes which are just used for one way, i.e. there are mostly nodes on the
 * beginning/end of a way or where ways cross each other.
 */
public class OptimizedNodeEdgeReader extends AbstractNodeEdgeReader {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedNodeEdgeReader.class);

    protected int optimizedWaysCounter = 0;
    protected int edgeCounter = 0;


    public OptimizedNodeEdgeReader(String osmFile) {
        super(osmFile);
    }


    protected void optimizeWays() {
        logger.info("Before optimization: " + this.nodeCounter.size() + " nodes have to be imported.");

        this.ways.forEach(wayObj -> {
            Way way = (Way) wayObj;
            final TLongList oldNodes = way.getNodes();
            final int oldNodesSize = oldNodes.size();
            final TLongList newNodes = new TLongArrayList(oldNodesSize);
            newNodes.add(oldNodes.get(0));

            // eliminate all nodes which are just used for this way so that less data needs to be imported;
            // also count edges which have to be imported so that we can create the graph
            for (int j = 1; oldNodesSize > 2 && j < oldNodesSize - 1; j++) {
                final long currentNode = oldNodes.get(j);
                if (this.nodeCounter.containsKey(currentNode) && this.nodeCounter.get(currentNode) > 1) {
                    newNodes.add(currentNode);
                    // consider if the way is one way or has both directions
                    this.edgeCounter += this.validator.isNotOneWay(way) ? 2 : 1;
                } else {
                    this.nodeCounter.remove(currentNode);
                }
            }

            // don't forget the connection to the last node
            newNodes.add(oldNodes.get(oldNodesSize - 1));
            this.edgeCounter += this.validator.isNotOneWay(way) ? 2 : 1;

            // update the node list of the way
            oldNodes.clear();
            oldNodes.addAll(newNodes);
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


    protected void readEdgesOfWays() {
        this.ways.forEach(wayObj -> {
            Way way = (Way) wayObj;
            final TLongList oldNodes = way.getNodes();
            final int oldNodesSize = oldNodes.size();
            long sourceNode = oldNodes.get(0);

            for (int j = 1; oldNodesSize > 2 && j < oldNodesSize - 1; j++) {
                long nodeId = oldNodes.get(j);
                // ignore nodes which just exist once in the graph and remove them from the node counter;
                // otherwise interpret it as an edge and add it to the graph
                if (this.nodeCounter.containsKey(nodeId) && this.nodeCounter.get(nodeId) > 1) {
                    addEdgeToGraph(way, sourceNode, nodeId);
                    // set the source node to the current one for the next possible edge
                    sourceNode = nodeId;
                } else {
                    this.nodeCounter.remove(nodeId);
                }
            }

            // this edge is either the whole way or the last edge of a split way
            addEdgeToGraph(way, sourceNode, oldNodes.get(oldNodesSize - 1));
        });

        logger.info("Finished import: Imported " + this.edgeCounter + " edges.");
        logger.debug("--- Graph ---\n" + this.graph.toSampleString());
    }
}