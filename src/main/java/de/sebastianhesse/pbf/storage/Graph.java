package de.sebastianhesse.pbf.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


/**
 * A Graph object contains nodes and edges. First add nodes {@link #addNode(Node)} and edges {@link #addEdge(Edge)},
 * then call {@link #sortAndConnectData()} which sorts the edges and adds an offset pointer to each node.
 * An offset pointer marks where the edges of a node start in the edge array.
 */
public class Graph {

    private static final Logger logger = LoggerFactory.getLogger(Graph.class);

    private Node[] nodes = null;
    private Edge[] edges = null;

    private int nodeIdx = 0;
    private int edgeIdx = 0;


    public Graph(int nodes, int edges) {
        this.nodes = new Node[nodes];
        this.edges = new Edge[edges];
    }


    /**
     * Adds a new edge to the graph and returns the index where it has been inserted.
     *
     * @param edge an edge containing at least the source and target id of a node
     * @return the index where the edge has been inserted at
     */
    public int addEdge(Edge edge) {
        this.edges[this.edgeIdx] = edge;
        return this.edgeIdx++;
    }


    /**
     * Adds a new node to the graph and return the index where it has been inserted.
     *
     * @param node a node containing at least latitude and longitude values
     * @return the index where the node has been inserted at
     */
    public int addNode(Node node) {
        this.nodes[this.nodeIdx] = node;
        return this.nodeIdx++;
    }


    /**
     * Sorts the data according to their source and target node id's. Also connects nodes and edges by setting an
     * offset pointer for each node. An offset pointer marks where the edges of a node start in the edge array.
     *
     * @return the updated graph object (this)
     */
    public Graph sortAndConnectData() {
        logger.debug("--- Start sorting ---");
        logger.debug("1. Sort edges according to their source node...");

        sortEdgesBasedOnSourceAndTarget();

        logger.debug("Done.");
        logger.debug("2. Set offset pointer to nodes...");

        setOffsetPointerToNodes();

        logger.debug("Done.");
        logger.debug("--- End sorting ---");

        return this;
    }


    private void sortEdgesBasedOnSourceAndTarget() {
        Arrays.sort(this.edges, (edge1, edge2) -> {
            if (edge1 == null || edge2 == null) {
                return 0;
            }
            if (edge1.getSourceNode() < edge2.getSourceNode() ||
                    (edge1.getSourceNode() == edge2.getSourceNode() && edge1.getTargetNode() < edge2.getTargetNode())) {
                return -1;
            } else if (edge1.getSourceNode() == edge2.getSourceNode() && edge1.getTargetNode() == edge2.getTargetNode()) {
                return 0;
            } else {
                return 1;
            }
        });
    }


    private void setOffsetPointerToNodes() {
        int j = 0;
        for (int i = 0; i < this.nodeIdx; i++) {
            Node node = this.nodes[i];
            boolean foundOffset = false;
            for (; j < this.edgeIdx; j++) {
                Edge edge = this.edges[j];
                if (!foundOffset && edge.getSourceNode() == i) {
                    node.setOffsetPointer(j);
                    foundOffset = true;
                } else if (foundOffset && edge.getSourceNode() != i) {
                    break;
                }
            }
        }
    }


    /**
     * @return the first 100 items of nodes and edges
     */
    public String toSampleString() {
        StringBuilder builder = new StringBuilder("--- Nodes: ").append(this.nodeIdx).append(" ---\n");
        for (int i = 0; i < 100 && i < this.nodes.length; i++) {
            Node node = this.nodes[i];
            builder.append(node.toString()).append("\n");
        }

        builder.append("\n").append("--- Edges: ").append(this.edgeIdx).append(" ---\n");
        for (int i = 0; i < 100 && i < this.edges.length; i++) {
            Edge edge = this.edges[i];
            builder.append(edge.toString()).append("\n");
        }
        return builder.toString();
    }
}
