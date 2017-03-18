package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.Dijkstra;


/**
 * Describe the neighbour and the weight to it. Is used within {@link Dijkstra} to update the distance to a neighbour.
 */
public class CalculationResult {

    public int neighbourId;
    public double weight;
    public double distance;
    // time how long it takes to drive distance
    public double distanceTime;


    public CalculationResult(int neighbourId, double weight, double distance, double distanceTime) {
        this.neighbourId = neighbourId;
        this.weight = weight;
        this.distance = distance;
        this.distanceTime = distanceTime;
    }
}
