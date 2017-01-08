package de.sebastianhesse.pbf.routing.calculators;

/**
 *
 */
public class CalculationResult {

    public int neighbourId;
    public double weight;


    public CalculationResult(int neighbourId, double weight) {
        this.neighbourId = neighbourId;
        this.weight = weight;
    }
}
