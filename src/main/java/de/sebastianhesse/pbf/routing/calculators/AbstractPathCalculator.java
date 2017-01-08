package de.sebastianhesse.pbf.routing.calculators;

import gnu.trove.map.TIntDoubleMap;


/**
 *
 */
public abstract class AbstractPathCalculator {

    private TIntDoubleMap distances;


    public AbstractPathCalculator(TIntDoubleMap distances) {
        this.distances = distances;
    }


    protected double getExistingDistance(int nodeId) {
        if (distances.containsKey(nodeId)) {
            return distances.get(nodeId);
        } else {
            return Double.MAX_VALUE;
        }
    }
}
