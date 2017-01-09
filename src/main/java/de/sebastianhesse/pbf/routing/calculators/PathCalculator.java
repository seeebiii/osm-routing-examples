package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;

import java.util.Optional;


/**
 * A calculator to process a strategy to check and compare the weight of a way. A strategy might be to find the
 * shortest or the fastest way (maybe depending on the vehicle as well).
 */
public interface PathCalculator {

    /**
     * Checks the costs to reach the neighbour {@link Edge#getTargetNode()} from {@code node}. If costs are below
     * existing costs to {@link Edge#getTargetNode()}, the return value contains a {@link CalculationResult}.
     *
     * @param node current visited node in Dijkstra
     * @param edge edge from node to a neighbour
     * @return maybe a {@link CalculationResult} if costs from {@code node} to neighbour are lower than existing costs
     */
    Optional<CalculationResult> calculateCostsToNeighbour(Node node, Edge edge);
}
