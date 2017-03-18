package de.sebastianhesse.pbf.routing;

import com.google.common.collect.Lists;
import de.sebastianhesse.pbf.routing.calculators.CalculationResult;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * A simple Dijkstra implementation.
 */
public class Dijkstra extends BaseDijkstra {

    private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);

    private Node target;
    private Map<Node, Node> targetCrossings;
    private Node finalTargetCrossing = null;
    private TIntIntMap crossingStarts;


    public Dijkstra(Graph graph, Node source, Node target, DijkstraOptions options) {
        super(graph, source, options);
        this.target = target;
        this.targetCrossings = new HashMap<>();
        this.crossingStarts = new TIntIntHashMap(this.nodes.length / 2);
    }


    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Dijkstra.");

        TLongSet settled = new TLongHashSet();
        FibonacciHeap<Integer> unsettled = new FibonacciHeap<>();
        unsettled.enqueue((int) source.getId(), 0d);
        weights.put((int) source.getId(), 0);
        predecessors.put((int) source.getId(), -1);

        findNextTargetCrossings();

        while (!unsettled.isEmpty()) {
            Node node = nodes[unsettled.dequeueMin().getValue()];

            if (settled.contains(node.getId())) {
                // we've already visited this node, thus skip it;
                continue;
            }

            if (node.equals(target)) {
                // stop here if we've found the target
                break;
            }

            if (targetCrossings.containsKey(node)) {
                this.finalTargetCrossing = node;
                break;
            }

            // investigate all neighbours of the current node and update the weights, predecessors, etc
            List<Edge> neighbours = this.graph.getNeighboursOfNode(node, settled);
            iterateOverNeighbours(unsettled, node, neighbours);

            // we investigated all of the node's neighbours -> mark node as visited
            settled.add(node.getId());
        }

        if (!weights.containsKey((int) target.getId()) && finalTargetCrossing == null) {
            logger.info("Can't find a way to target.");
            predecessors.clear();
            weights.clear();
        }

        logger.info("Finished Dijkstra in {} ms.", (System.currentTimeMillis() - startTime));
    }


    private void findNextTargetCrossings() {
        if (!target.isCrossing()) {
            List<Edge> neighbours = this.graph.getNeighboursOfNode(this.target, new TLongHashSet());
            for (Edge edge : neighbours) {
                int nextCrossing = edge.getNextCrossing();
                if (nextCrossing > -1) {
                    this.targetCrossings.put(this.nodes[nextCrossing], this.nodes[edge.getTargetNode()]);
                }
            }
        }
    }


    private void iterateOverNeighbours(FibonacciHeap<Integer> unsettled, Node node, List<Edge> neighbours) {
        for (Edge edge : neighbours) {
            int targetNodeId = edge.getNextCrossing() > -1 ? edge.getNextCrossing() : edge.getTargetNode();
            Node crossingNode = edge.getNextCrossing() > -1 ? this.nodes[targetNodeId] : null;

            try {
                Optional<CalculationResult> result = this.pathCalculator.calculateCostsToNeighbour(node, edge, crossingNode);
                result.ifPresent(calculationResult -> {
                    weights.put(targetNodeId, calculationResult.weight);
                    predecessors.put(targetNodeId, (int) node.getId());
                    if (edge.getNextCrossing() > -1) {
                        // if current edge has a shortcut to a next crossing, we need to save the starting node
                        crossingStarts.put(targetNodeId, edge.getTargetNode());
                    }
                    // always put a new object on the heap and avoid a costly decreaseKey operation
                    // -> when polling the heap, make sure to check if we already visited the node
                    unsettled.enqueue(targetNodeId, calculationResult.weight);
                });
            } catch (Exception e) {
                Node neighbour = nodes[targetNodeId];
                logger.info("Exception occurred. Current node: {}, neighbours: {}, current neighbour: {}",
                        node.toString(), neighbours.size(), neighbour.toString());
                logger.error("Exception: ", e);
            }
        }
    }


    public List<Node> retrieveShortestPath() {
        if (predecessors.isEmpty()) {
            return Lists.newArrayList();
        }

        long startTime = System.currentTimeMillis();

        List<Node> path = new ArrayList<>(predecessors.size());
        Node routeNode = target;
        boolean avoidAddingRouteNode = false;
        if (!target.isCrossing() && finalTargetCrossing != null) {
            path.add(target);
            avoidAddingRouteNode = addNodesFromTargetToNextCrossing(path, finalTargetCrossing, target);
            routeNode = finalTargetCrossing;
        }
        while (isPredecessor(routeNode)) {
            if (!avoidAddingRouteNode) {
                path.add(routeNode);
            }
            Node tmpTarget = routeNode;
            routeNode = getPredecessor(routeNode);
            if (routeNode.isCrossing() || routeNode.equals(this.source)) {
                avoidAddingRouteNode = addNodesFromTargetToNextCrossing(path, routeNode, tmpTarget);
            }
        }
        path.add(this.source);
        // nodes are ordered from target to source, but we want them in the direction from source to target
        Collections.reverse(path);
        logger.info("It took {} ms to generate the whole list of points.", (System.currentTimeMillis() - startTime));
        return path;
    }


    private static String getPath(List<Node> path) {
        StringBuilder builder = new StringBuilder("[");
        path.forEach(node -> builder.append("[").append(node.getLat()).append(",").append(node.getLon()).append("],"));
        return builder.append("]").toString();
    }


    private boolean addNodesFromTargetToNextCrossing(List<Node> path, Node startCrossing, Node target) {
        if (startCrossing.isCrossing() && !this.crossingStarts.containsKey((int) target.getId())) {
            // TODO why this branch?
            return false;
        }

        // retrieve the first node of the way between the final target crossing to the target
        Node startNode = null;
        if (this.crossingStarts.containsKey((int) target.getId())) { // TODO USE TARGET ???
            startNode = this.nodes[this.crossingStarts.get((int) target.getId())];
            // don't use crossings in this case as they have > 1 neighbours to start with...
            if (startNode.equals(startCrossing) || startNode.equals(target)) {
                return false;
            }
        } else {
            // in case startCrossing is not a crossing (e.g. it's the source node), just start with this node
            startNode = startCrossing;
        }

        boolean avoidAddingStartCrossing = false;

        // then get all nodes between them
        List<Node> nodes = this.graph.getNodesOfSimpleWay(startNode, target);
        // nodes are order from crossing to target, but path expects the nodes to be in the other direction
        Collections.reverse(nodes);
        path.addAll(nodes);
        // return finalTargetCrossing which our new starting point
        return avoidAddingStartCrossing;
    }
}
