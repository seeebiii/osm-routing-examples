package de.sebastianhesse.pbf.routing;

import com.google.common.collect.Lists;
import de.sebastianhesse.pbf.reader.Accessor;
import de.sebastianhesse.pbf.routing.accessors.CarAccessor;
import de.sebastianhesse.pbf.routing.accessors.PedestrianAccessor;
import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.routing.calculators.CalculationResult;
import de.sebastianhesse.pbf.routing.calculators.FastestPathCalculator;
import de.sebastianhesse.pbf.routing.calculators.PathCalculator;
import de.sebastianhesse.pbf.routing.calculators.ShortestPathCalculator;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * A simple Dijkstra implementation.
 */
public class Dijkstra extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);

    private Graph graph;
    private Node source;
    private Node target;
    private Node[] nodes;

    // store costs
    private TIntDoubleMap distances;
    // store nodes of shortest path
    private TIntIntMap predecessors;
    private PathCalculator pathCalculator;
    private DijkstraOptions options = DijkstraOptions.shortestWithCar();


    public Dijkstra(Graph graph, Node source, Node target, DijkstraOptions options) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.nodes = this.graph.getNodes();
        this.distances = new TIntDoubleHashMap(this.nodes.length);
        this.predecessors = new TIntIntHashMap(this.nodes.length);
        this.options = options;
        this.pathCalculator = getPathCalculator();
    }


    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Dijkstra.");

        TLongSet settled = new TLongHashSet();
        FibonacciHeap<Integer> unsettled = new FibonacciHeap<>();
        unsettled.enqueue((int) source.getId(), 0d);
        distances.put((int) source.getId(), 0);
        predecessors.put((int) source.getId(), -1);

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

            // investigate all neighbours of the current node and update the distances, predecessors, etc
            List<Edge> neighbours = this.graph.getNeighboursOfNode(node, settled);
            for (Edge edge : neighbours) {
                int targetNodeId = edge.getTargetNode();
                try {
                    Optional<CalculationResult> result = this.pathCalculator.calculateCostsToNeighbour(node, edge);
                    result.ifPresent(calculationResult -> {
                        distances.put(targetNodeId, calculationResult.weight);
                        predecessors.put(targetNodeId, (int) node.getId());
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

            // we investigated all of the node's neighbours -> mark node as visited
            settled.add(node.getId());
        }

        if (distances.containsKey((int) target.getId())) {
            logger.info("Distance to target: {}", distances.get((int) target.getId()));
        } else {
            logger.info("Can't find a way to target.");
            predecessors.clear();
            distances.clear();
        }

        logger.info("Finished Dijkstra in {} ms.", (System.currentTimeMillis() - startTime));
    }


    public List<Node> retrieveShortestPath() {
        if (predecessors.isEmpty()) {
            return Lists.newArrayList();
        }

        List<Node> path = new ArrayList<>(predecessors.size());
        Node routeNode = target;
        while (predecessors.containsKey((int) routeNode.getId()) && predecessors.get((int) routeNode.getId()) != -1) {
            path.add(routeNode);
            routeNode = graph.getNodes()[predecessors.get((int) routeNode.getId())];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }


    private PathCalculator getPathCalculator() {
        WayAccessor accessor;
        if (options.getAccessor().equals(Accessor.CAR)) {
            accessor = new CarAccessor();
        } else {
            accessor = new PedestrianAccessor();
        }

        switch (options.getCalculationType()) {
            case FASTEST:
                return new FastestPathCalculator(this.distances, accessor);
            case SHORTEST:
                return new ShortestPathCalculator(this.distances, accessor);
            default:
                throw new IllegalStateException("Dijkstra options have a mismatching state: neither fastest nor shortest type was selected.");
        }
    }
}
