package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.util.GraphUtil;
import gnu.trove.map.TIntDoubleMap;

import java.util.Optional;


/**
 * Implements a fastest path approach for Dijkstra. Costs are: distance / speed
 */
public class FastestPathCalculator extends AbstractPathCalculator {


    public FastestPathCalculator(TIntDoubleMap distances, WayAccessor wayAccessor) {
        super(distances, wayAccessor);
    }


    @Override
    public Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge, Node crossingNode) {
        if (edge.getSpeed() > 0) {
            int targetNodeId = crossingNode == null ? edge.getTargetNode() : (int) crossingNode.getId();
            double distance = crossingNode == null ? edge.getDistance() : GraphUtil.getDistance(node, crossingNode);
            double weightToNeighbour = getExistingWeight((int) node.getId()) + distance / edge.getSpeed();
            if (getExistingWeight(targetNodeId) > weightToNeighbour) {
                return Optional.of(new CalculationResult(targetNodeId, weightToNeighbour));
            }
        }
        return Optional.empty();
    }
}
