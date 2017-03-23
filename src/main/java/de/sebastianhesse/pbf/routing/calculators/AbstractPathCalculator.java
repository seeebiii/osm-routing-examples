package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.accessors.WayAccessor;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;
import gnu.trove.map.TIntDoubleMap;

import java.util.Optional;


/**
 * Abstract class for {@link PathCalculator}s providing basic functionality.
 */
public abstract class AbstractPathCalculator implements PathCalculator {

    private TIntDoubleMap distances;
    private WayAccessor wayAccessor;


    public AbstractPathCalculator(TIntDoubleMap distances, WayAccessor wayAccessor) {
        this.distances = distances;
        this.wayAccessor = wayAccessor;
    }


    protected double getExistingWeight(int nodeId) {
        if (distances.containsKey(nodeId)) {
            return distances.get(nodeId);
        } else {
            return Double.MAX_VALUE;
        }
    }


    @Override
    public Optional<CalculationResult> calculateCostsToNeighbour(Node node, Edge edge, Node crossingNode) {
        // just allow this way if the edge can be accessed and it's not closed
        if (wayAccessor.canAccessWay(edge) && edge.getAdditionalWeight() < 1.0) {
            return checkNeighbourAndCosts(node, edge, crossingNode);
        } else {
            return Optional.empty();
        }
    }


    protected abstract Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge, Node crossingNode);


    /**
     * @param edge current edge to check
     * @return the edge speed (or the max. speed of the way accessor) in meter per seconds
     */
    protected double getSpeedInMeterPerSeconds(Edge edge) {
        short speed = edge.getSpeed();
        if (speed > wayAccessor.getMaxSpeed()) {
            speed = wayAccessor.getMaxSpeed();
        }
        return speed / 3.6;
    }
}
