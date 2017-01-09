package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
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
    public Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge) {
        if (edge.getSpeed() > 0) {
            double weightToNeighbour = getExistingDistance((int) node.getId()) + edge.getDistance() / edge.getSpeed();
            if (getExistingDistance(edge.getTargetNode()) > weightToNeighbour) {
                return Optional.of(new CalculationResult(edge.getTargetNode(), weightToNeighbour));
            }
        }
        return Optional.empty();
    }
}
