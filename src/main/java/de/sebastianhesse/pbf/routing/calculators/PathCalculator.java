package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Node;

import java.util.Optional;


/**
 *
 */
public interface PathCalculator {

    Optional<CalculationResult> checkNeighbourAndCosts(Node node, Edge edge);
}
