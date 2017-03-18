package de.sebastianhesse.pbf.routing;

import de.sebastianhesse.pbf.reader.Accessor;
import de.sebastianhesse.pbf.routing.accessors.CarAccessor;
import de.sebastianhesse.pbf.routing.accessors.PedestrianAccessor;
import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.routing.calculators.FastestPathCalculator;
import de.sebastianhesse.pbf.routing.calculators.PathCalculator;
import de.sebastianhesse.pbf.routing.calculators.ShortestPathCalculator;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;


/**
 *
 */
public abstract class BaseDijkstra extends Thread {

    protected Graph graph;
    protected Node source;
    protected Node[] nodes;
    protected DijkstraOptions options = DijkstraOptions.shortestWithCar();
    protected PathCalculator pathCalculator;

    // store costs
    protected TIntDoubleMap weights;
    // store nodes of shortest path
    protected TIntIntMap predecessors;


    public BaseDijkstra(Graph graph, Node source, DijkstraOptions options) {
        this.graph = graph;
        this.source = source;
        this.nodes = this.graph.getNodes();
        this.weights = new TIntDoubleHashMap(this.nodes.length / 2);
        this.predecessors = new TIntIntHashMap(this.nodes.length / 2);
        this.options = options;
        this.pathCalculator = getPathCalculator();
    }


    protected Node getPredecessor(Node routeNode) {
        return graph.getNodes()[predecessors.get((int) routeNode.getId())];
    }


    protected boolean isPredecessor(Node routeNode) {
        return predecessors.containsKey((int) routeNode.getId()) && predecessors.get((int) routeNode.getId()) != -1;
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
                return new FastestPathCalculator(this.weights, accessor);
            case SHORTEST:
                return new ShortestPathCalculator(this.weights, accessor);
            default:
                throw new IllegalStateException("Dijkstra options have a mismatching state: neither fastest nor shortest type was selected.");
        }
    }
}
