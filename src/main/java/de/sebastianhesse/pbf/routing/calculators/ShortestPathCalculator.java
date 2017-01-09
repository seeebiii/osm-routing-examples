package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntDoubleMap;

import java.util.Optional;


/**
 * Implements a shortest path approach for Dijkstra. Costs: distance
 */
public class ShortestPathCalculator extends AbstractPathCalculator {

    public ShortestPathCalculator(TIntDoubleMap distances, WayAccessor wayAccessor) {
        super(distances, wayAccessor);
    }


    @Override
    public Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge) {
        int targetNodeId = edge.getTargetNode();
        double calcDistanceToNeighbour = getExistingDistance((int) node.getId()) + edge.getDistance();
        if (getExistingDistance(targetNodeId) > calcDistanceToNeighbour) {
            return Optional.of(new CalculationResult(targetNodeId, calcDistanceToNeighbour));
        }
        return Optional.empty();
    }
}
