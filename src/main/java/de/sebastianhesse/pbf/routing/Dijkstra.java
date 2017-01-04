package de.sebastianhesse.pbf.routing;

import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;


/**
 * A simple Dijkstra implementation.
 */
public class Dijkstra {

    private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);

    private Graph graph;
    private Node source;
    private Node target;
    private Node[] nodes;

    // store costs
    private TObjectDoubleMap<Node> distances;
    // store nodes of cheapest path
    private TObjectIntMap<Node> predecessors;


    public Dijkstra(Graph graph, Node source, Node target) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.nodes = this.graph.getNodes();
        this.distances = new TObjectDoubleHashMap<>(this.nodes.length);
        this.predecessors = new TObjectIntHashMap<>(this.nodes.length);
    }


    public List<Node> getShortestPath() {
        logger.info("Starting Dijkstra.");

        TLongSet settled = new TLongHashSet();
        PriorityQueue<Node> unsettled = new PriorityQueue<>();
        unsettled.add(source);
        distances.put(source, 0);
        predecessors.put(source, -1);

        while (!unsettled.isEmpty()) {
            Node node = getMinimum(unsettled);
            settled.add(node.getId());
            unsettled.remove(node);

            if (node.equals(target)) {
                break;
            }

            List<Node> neighbours = this.graph.getNeighboursOfNode(node, settled);
            for (Node neighbour : neighbours) {
                try {
                    double calcDistanceToNeighbour = getShortestDistance(node) + getDistance(node, neighbour);
                    if (getShortestDistance(neighbour) > calcDistanceToNeighbour) {
                        distances.put(neighbour, calcDistanceToNeighbour);
                        predecessors.put(neighbour, (int) node.getId());
                        unsettled.add(neighbour);
                    }
                } catch (Exception e) {
                    logger.info("Exception occurred. Current node: {}, neighbours: {}, current neighbour: {}",
                            node.toString(), neighbours.size(), neighbour.toString());
                    logger.error("Exception: ", e);
                    unsettled.clear();
                }
            }
        }

        if (distances.containsKey(target)) {
            logger.info("Distance to target: {}", distances.get(target));
        } else {
            logger.info("Can't find a way to target.");
        }

        logger.info("Finished Dijkstra.");

        return getNodesFromPredecessors();
    }


    private double getDistance(Node source, Node target) {
        Edge[] edges = this.graph.getEdges();
        for (int i = source.getOffsetPointer(); i < graph.getEdgesSize(); i++) {
            Edge edge = edges[i];
            if (edge.getSourceNode() == source.getId() && edge.getTargetNode() == target.getId()) {
                return edge.getDistance();
            } else if (edge.getSourceNode() != source.getId()) {
                // because of the ordering of the edges (ordered by node id) we can stop here
                // if there are no further edges for the source node
                break;
            }
        }
        throw new IllegalStateException("Found two nodes which are not connected with an edge.");
    }


    private Node getMinimum(PriorityQueue<Node> unsettled) {
        Node minimum = null;
        for (Node node : unsettled) {
            if (minimum == null) {
                minimum = node;
            } else if (getShortestDistance(node) < getShortestDistance(minimum)) {
                minimum = node;
            }
        }
        return minimum;
    }


    private double getShortestDistance(Node node) {
        if (distances.containsKey(node)) {
            return distances.get(node);
        } else {
            return Double.MAX_VALUE;
        }
    }

    private List<Node> getNodesFromPredecessors() {
        List<Node> path = new ArrayList<>(predecessors.size());
        Node routeNode = target;
        while (predecessors.containsKey(routeNode) && predecessors.get(routeNode) != -1) {
            path.add(routeNode);
            routeNode = graph.getNodes()[predecessors.get(routeNode)];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }


    public TObjectIntMap<Node> getPredecessors() {
        return predecessors;
    }
}
