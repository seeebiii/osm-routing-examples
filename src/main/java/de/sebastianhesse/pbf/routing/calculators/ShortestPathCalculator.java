package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntDoubleMap;

import java.util.Optional;


/**
 *
 */
public class ShortestPathCalculator extends AbstractPathCalculator implements PathCalculator {

    public ShortestPathCalculator(TIntDoubleMap distances) {
        super(distances);
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
