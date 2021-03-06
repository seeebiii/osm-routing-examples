package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.util.GraphUtil;
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
    public Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge, Node crossingNode) {
        int targetNodeId = crossingNode == null ? edge.getTargetNode() : (int) crossingNode.getId();
        // if we're using a shortcut, we have to calc the distance on the fly
        double distance = crossingNode == null ? edge.getDistance() : GraphUtil.getDistance(node, crossingNode);
        double calcDistanceToNeighbour = getExistingWeight((int) node.getId()) + distance;
        if (getExistingWeight(targetNodeId) > calcDistanceToNeighbour) {
            return Optional.of(new CalculationResult(targetNodeId, calcDistanceToNeighbour, distance,
                    distance / getSpeedInMeterPerSeconds(edge)));
        }
        return Optional.empty();
    }
}
