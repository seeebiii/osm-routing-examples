package de.sebastianhesse.pbf.storage;

import de.sebastianhesse.pbf.exceptions.OutOfRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


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

    private Map<String, GridOffset> gridOffsets;
    private GraphBoundary graphBoundary;


    public Graph(int nodes, int edges) {
        this.nodes = new Node[nodes];
        this.edges = new Edge[edges];
        this.gridOffsets = new HashMap<>();
        this.graphBoundary = new GraphBoundary();
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
     * Creates a grid based on lat and lon. E.g. all points of 48.x/10.x are in one grid cell.
     * Also calculates the boundaries of the available OSM data.
     *
     * @return
     */
    public Graph sortNodesAndSetGraphBoundaries() {
        sortNodesIntoGridCells();
        calcGridOffsetsOnNodesAndUpdateBoundaries();

        return this;
    }


    private void sortNodesIntoGridCells() {
        Arrays.sort(this.nodes, (node1, node2) -> {
            if (node1 == null || node2 == null) {
                return 0;
            }

            long lat1 = (long) node1.getLat();
            long lat2 = (long) node2.getLat();
            long lon1 = (long) node1.getLon();
            long lon2 = (long) node2.getLon();

            if (lat1 == lat2 && lon1 == lon2) {
                return 0;
            }

            if ((lat1 == lat2 && lon1 < lon2) || lat1 > lat2) {
                return -1;
            }

            if ((lat1 == lat2 && lon1 > lon2) || lat1 < lat2) {
                return 1;
            }

            return 0;
        });
    }


    private void calcGridOffsetsOnNodesAndUpdateBoundaries() {
        short lat = (short) this.nodes[0].getLat();
        short lon = (short) this.nodes[0].getLon();
        GridOffset lastOffset = new GridOffset(getGridCellName(lat, lon), 0);
        this.gridOffsets.put(lastOffset.getName(), lastOffset);

        for (int i = 1; i < this.nodes.length; i++) {
            Node node = this.nodes[i];
            this.graphBoundary.updateCorners(node);
            boolean updatedLatOrLong = false;

            // every time the grid coordinates change, we add another offset entry
            if (((short) node.getLon()) != lon) {
                lon = (short) node.getLon();
                updatedLatOrLong = true;
            }

            if (((short) node.getLat()) != lat) {
                lat = (short) node.getLat();
                updatedLatOrLong = true;
            }

            // only update the offset if the current value belongs to the next grid
            if (updatedLatOrLong) {
                String nextOffsetName = getGridCellName(lat, lon);
                lastOffset.setNextOffset(nextOffsetName);
                this.gridOffsets.put(lastOffset.getName(), lastOffset);
                lastOffset = new GridOffset(nextOffsetName, i);
                this.gridOffsets.put(lastOffset.getName(), lastOffset);
            }
        }
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


    public Node[] getNodes() {
        return nodes;
    }


    public Edge[] getEdges() {
        return edges;
    }


    public Node[][] getGraphBoundaries() {
        return this.graphBoundary.getBoundaryNodes();
    }


    public Optional<Node> findClosestNode(double lat, double lon) {
        try {
            GridOffset offset = getGridCellOffset(lat, lon);
            double latDiff = 1.0;
            double lonDiff = 1.0;
            Node selectedNode = null;
            for (int i = offset.getOffset(); i < this.nodes.length; i++) {
                Node node = this.nodes[i];
                if (node.getLat() == lat && node.getLon() == lon) {
                    return Optional.of(node);
                }

                // TODO: optimize
                double tmpLatDiff = Math.abs(lat - node.getLat());
                double tmpLonDiff = Math.abs(lon - node.getLon());
                if (tmpLatDiff < latDiff && tmpLonDiff < lonDiff) {
                    selectedNode = node;
                    latDiff = tmpLatDiff;
                    lonDiff = tmpLonDiff;
                }
            }

            // if no match was found, just return an empty optional
            return Optional.ofNullable(selectedNode);
        } catch (OutOfRangeException e) {
            return Optional.empty();
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


    /**
     * Returns the {@link GridOffset} containing the first point of a grid cell in the node list.
     * If lat/lon can't be matched to a cell, it means this point is out of range.
     *
     * @param lat latitude of a point
     * @param lon longitude of a point
     * @return the {@link GridOffset} containing the first point of a grid cell
     * @throws OutOfRangeException if lat/lon can't be matched to a cell
     */
    private GridOffset getGridCellOffset(double lat, double lon) {
        if (this.gridOffsets.containsKey(getGridCellName(lat, lon))) {
            return this.gridOffsets.get(getGridCellName(lat, lon));
        }

        throw new OutOfRangeException("The lat and lon you've provided is out of range.");
    }


    private String getGridCellName(double lat, double lon) {
        return getGridCellName((short) lat, (short) lon);
    }


    private String getGridCellName(short lat, short lon) {
        return Short.toString(lat) + "," + Short.toString(lon);
    }
}