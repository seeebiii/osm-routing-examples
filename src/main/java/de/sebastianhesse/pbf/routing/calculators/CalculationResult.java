package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.Dijkstra;


/**
 * Describe the neighbour and the weight to it. Is used within {@link Dijkstra} to update the distance to a neighbour.
 */
public class CalculationResult {

    public int neighbourId;
    public double weight;


    public CalculationResult(int neighbourId, double weight) {
        this.neighbourId = neighbourId;
        this.weight = weight;
    }
}
