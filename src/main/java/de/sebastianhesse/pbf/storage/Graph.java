package de.sebastianhesse.pbf.storage;

import de.sebastianhesse.pbf.exceptions.OutOfRangeException;
import de.sebastianhesse.pbf.util.GraphUtil;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A Graph object contains nodes and edges. First add nodes {@link #addNode(Node)} and edges {@link #addEdge(Edge)},
 * then call {@link #sortAndConnectData()} which sorts the edges and adds an offset pointer to each node.
 * An offset pointer marks where the edges of a node start in the edge array.
 */
public class Graph {

    private static final Logger logger = LoggerFactory.getLogger(Graph.class);
    public static final double MAX_DIFF = 0.1;

    private Node[] nodes = null;
    private Edge[] edges = null;
    private TObjectLongMap<Node> pois = new TObjectLongHashMap<>();

    private int nodeIdx = 0;
    private int edgeIdx = 0;

    private Map<String, GridOffset> gridOffsets;
    private GraphBoundary graphBoundary;
    private Map<String, Set<String>> poiTypes;


    public Graph(int nodes, int edges) {
        this.nodes = new Node[nodes];
        this.edges = new Edge[edges];
        this.gridOffsets = new HashMap<>();
        this.graphBoundary = new GraphBoundary();
        this.poiTypes = new HashMap<>();
    }


    /**
     * Adds a new edge to the graph and returns the index where it has been inserted.
     *
     * @param edge an edge containing at least the source and target id of a node
     */
    public void addEdge(Edge edge) {
        if (edge != null && edge.getSourceNode() != edge.getTargetNode()) {
            this.edges[this.edgeIdx] = edge;
            this.edgeIdx++;
        }
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
     * @return current graph
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

            BigDecimal lat1 = BigDecimal.valueOf(node1.getLat());
            BigDecimal lat2 = BigDecimal.valueOf(node2.getLat());
            BigDecimal lon1 = BigDecimal.valueOf(node1.getLon());
            BigDecimal lon2 = BigDecimal.valueOf(node2.getLon());

            if (compare(lat1, lat2) == 0 && compare(lon1, lon2) == 0) {
                return 0;
            }

            if (compare(lat1, lat2) == 1 || (compare(lat1, lat2) == 0 && compare(lon1, lon2) == -1)) {
                return -1;
            }

            if (compare(lat1, lat2) == -1 || (compare(lat1, lat2) == 0 && compare(lon1, lon2) == 1)) {
                return 1;
            }

            return 0;
        });
    }


    private void calcGridOffsetsOnNodesAndUpdateBoundaries() {
        BigDecimal lat = BigDecimal.valueOf(this.nodes[0].getLat());
        BigDecimal lon = BigDecimal.valueOf(this.nodes[0].getLon());
        String gridCellName = getGridCellName(getGridCellPart(lat), getGridCellPart(lon));
        GridOffset lastOffset = new GridOffset(gridCellName, 0);
        this.gridOffsets.put(lastOffset.getName(), lastOffset);
        BigDecimal maxDiff = BigDecimal.valueOf(MAX_DIFF);

        for (int i = 1; i < this.nodes.length; i++) {
            Node node = this.nodes[i];
            if (node == null) {
                continue;
            }

            this.graphBoundary.updateCorners(node);
            boolean updatedLatOrLong = false;

            // every time the grid coordinates change, we add another offset entry
            BigDecimal lat1 = BigDecimal.valueOf(node.getLat());
            BigDecimal lon1 = BigDecimal.valueOf(node.getLon());

            if (isNewDecimalRange(lat, lat1, maxDiff)) {
                lat = BigDecimal.valueOf(node.getLat());
                updatedLatOrLong = true;
            }

            if (isNewDecimalRange(lon, lon1, maxDiff)) {
                lon = BigDecimal.valueOf(node.getLon());
                updatedLatOrLong = true;
            }


            // only update the offset if the current value belongs to the next grid
            if (updatedLatOrLong) {
                String nextOffsetName = getGridCellName(getGridCellPart(lat), getGridCellPart(lon));
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

        setOffsetPointerForNodes();

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


    private void setOffsetPointerForNodes() {
        int j = 0;
        boolean foundOffset = false;
        for (int i = 0; i < this.nodeIdx; i++) {
            Node node = this.nodes[i];
            updateNodeId(i, node);

            for (; j < this.edgeIdx; j++) {
                Edge edge = this.edges[j];
                if (!foundOffset && edge.getSourceNode() == i) {
                    node.setOffsetPointer(j);
                    foundOffset = true;
                } else if (edge.getSourceNode() != i) {
                    // reset foundOffset
                    foundOffset = false;
                    break;
                }
            }
        }
    }


    private void updateNodeId(int i, Node node) {
        // check if node is a POI and then also update the POI data; otherwise just update the id
        if (node.isPoi() && this.pois.containsKey(node)) {
            node.setId(i);
            this.pois.put(node, node.getId());
        } else {
            node.setId(i);
        }
    }


    public Node[] getNodes() {
        return nodes;
    }


    public Edge[] getEdges() {
        return edges;
    }


    public int getNodesSize() {
        return this.nodes.length;
    }


    public int getEdgesSize() {
        return this.edgeIdx;
    }


    public Node[][] getGraphBoundaries() {
        return this.graphBoundary.getBoundaryNodes();
    }


    public List<Edge> getNeighboursOfNode(Node node, TLongSet settled) {
        int edgeOffset = node.getOffsetPointer();

        if (edgeOffset == -1) {
            // this might happen if a street ends and the street has just one way/direction
            return new ArrayList<>();
        }

        List<Edge> neighbours = new ArrayList<>();
        for (int i = edgeOffset; i < this.edgeIdx; i++) {
            Edge edge = this.edges[i];
            if (edge.getSourceNode() == node.getId()) {
                if (!settled.contains(edge.getTargetNode())) {
                    neighbours.add(edge);
                }
            } else {
                // this happens if there are no further edges for the node, so we can break out and return neighbours
                break;
            }
        }

        return neighbours;
    }


    /**
     * Gets the way from a source node and a target node of a simple way. A simple way means that there is no other
     * way between those two points, i.e. there is no crossing between them.
     *
     * @param source the first node of the way
     * @param target the node to end the way with; must be reachable from source
     * @return list of nodes, exclusively source and target node;
     * if target is the direct successor of source, the list just contains the source node
     */
    public List<Node> getNodesOfSimpleWay(Node source, Node target) {
        List<Node> nodes = new ArrayList<>();
        if (source.equals(target)) {
            return nodes;
        }
        int offset = source.getOffsetPointer();
        Edge edge = this.edges[offset];
        while (edge.getTargetNode() != (int) target.getId()) {
            Node n = this.nodes[edge.getTargetNode()];
            List<Edge> neighbours = this.getNeighboursOfNode(n, new TLongHashSet());
            if (neighbours.size() > 1) {
                break;
            }
            offset = this.nodes[edge.getTargetNode()].getOffsetPointer();
            if (offset > -1) {
                edge = this.edges[offset];
                nodes.add(this.nodes[edge.getSourceNode()]);
            } else {
                break;
            }
        }
        return nodes;
    }


    public Optional<Node> findClosestNode(Node node) {
        if (node.getId() > -1) {
            return findClosestNode((int) node.getId(), node.getLat(), node.getLon());
        } else {
            return findClosestNode(node.getLat(), node.getLon());
        }
    }


    public Optional<Node> findClosestNode(int id, double lat, double lon) {
        if (id > -1 && id < this.nodes.length) {
            return Optional.ofNullable(this.nodes[id]);
        } else {
            return findClosestNode(lat, lon);
        }
    }


    public Optional<Node> findClosestNode(double lat, double lon) {
        try {
            List<GridOffset> cells = getGridCellsAround(lat, lon);
            Node searchNode = new Node(lat, lon);
            double distance = Double.MAX_VALUE;
            Node selectedNode = null;

            // search in all grids around the searchNode
            for (GridOffset offset : cells) {
                GridOffset nextOffset = null;
                int upperBound = nodeIdx;
                try {
                    nextOffset = getGridCellOffset(offset.getNextOffset());
                    upperBound = nextOffset.getOffset();
                } catch (OutOfRangeException e) {
                    logger.debug("No next offset available.");
                }

                for (int i = offset.getOffset(); i < upperBound; i++) {
                    Node node = this.nodes[i];
                    if (node.getLat() == lat && node.getLon() == lon) {
                        return Optional.of(node);
                    }

                    double tmpDistance = Math.abs(GraphUtil.getDistance(searchNode, node));
                    if (tmpDistance < distance) {
                        selectedNode = node;
                        distance = tmpDistance;
                    }
                }
            }

            // maybe we still haven't found the node yet, thus use ofNullable
            return Optional.ofNullable(selectedNode);
        } catch (OutOfRangeException e) {
            // if grid cell was not found, just return an empty optional
            return Optional.empty();
        }
    }


    private List<GridOffset> getGridCellsAround(double lat, double lon) {
        BigDecimal lat1 = BigDecimal.valueOf(lat);
        BigDecimal lon1 = BigDecimal.valueOf(lon);
        List<GridOffset> offsets = new ArrayList<>(9);

        for (short i = -1; i < 2; i++) {
            for (short j = -1; j < 2; j++) {
                try {
                    BigDecimal lat2 = BigDecimal.valueOf(i * MAX_DIFF);
                    BigDecimal lon2 = BigDecimal.valueOf(j * MAX_DIFF);
                    offsets.add(getGridCellOffset(getGridCellName(lat1.subtract(lat2), lon1.add(lon2))));
                } catch (OutOfRangeException e) {
                    // do nothing, it just means we're out of the grid range
                }
            }
        }

        return offsets;
    }


    public void addPoi(Node node, long idx) {
        // if we already know the node, then use that node object instead of storing a copy
        if (idx > -1 && idx < this.nodes.length) {
            int i = Long.valueOf(idx).intValue();
            Node existingNode = this.nodes[i];
            existingNode.setPoi(true);
            existingNode.setType(node.getTypeKey(), node.getTypeValue());
            this.pois.put(existingNode, idx);
        } else {
            this.pois.put(node, idx);
        }
    }


    public void addPoi(Node node) {
        this.addPoi(node, -1);
    }


    public List<Node> getPoisAround(Node source, short maxDistance, Pair<String, String> type) {
        return this.pois.keySet().parallelStream()
                .filter(node -> GraphUtil.getDistance(source, node) <= maxDistance * 1000 &&
                        node.isTypeOf(type.getKey(), type.getValue()))
                .collect(Collectors.toList());
    }


    public void setPoiTypes(Map<String, Set<String>> poiTypes) {
        this.poiTypes = poiTypes;
    }


    public Map<String, Set<String>> getPoiTypes() {
        return poiTypes;
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
        for (int i = 0; i < 100 && i < this.edgeIdx; i++) {
            Edge edge = this.edges[i];
            builder.append(edge.toString()).append("\n");
        }
        return builder.toString();
    }


    /**
     * Returns the {@link GridOffset} containing the first point of a grid cell in the node list.
     * If lat/lon can't be matched to a cell, it means this point is out of range.
     *
     * @param name comma separated lat lon value
     * @return the {@link GridOffset} containing the first point of a grid cell
     * @throws OutOfRangeException if lat/lon can't be matched to a cell
     */
    private GridOffset getGridCellOffset(String name) throws OutOfRangeException {
        if (StringUtils.isBlank(name) || !this.gridOffsets.containsKey(name)) {
            throw new OutOfRangeException("The lat and lon you've provided is out of range.");
        }
        return this.gridOffsets.get(name);
    }


    private String getGridCellName(BigDecimal one, BigDecimal two) {
        return getGridCellPart(one) + "," + getGridCellPart(two);
    }


    private String getGridCellName(String lat, String lon) {
        return lat + "," + lon;
    }


    private String getGridCellPart(BigDecimal decimal) {
        return getScaledDecimal(decimal, 1).toString();
    }


    private boolean isNewDecimalRange(BigDecimal one, BigDecimal two, BigDecimal maxDiff) {
        // number of decimal parts, e.g 1.123 => 3 decimal parts
        int roundingSize = 1;
        one = getScaledDecimal(one, roundingSize);
        two = getScaledDecimal(two, roundingSize);
        return one.subtract(two).abs().compareTo(maxDiff) > -1;
    }


    private int compare(BigDecimal one, BigDecimal two) {
        int roundingSize = 1;
        one = getScaledDecimal(one, roundingSize);
        two = getScaledDecimal(two, roundingSize);
        return one.compareTo(two);
    }


    private BigDecimal getScaledDecimal(BigDecimal decimal, int roundingSize) {
        return decimal.setScale(roundingSize, BigDecimal.ROUND_DOWN);
    }
}
