package de.sebastianhesse.pbf.routing;

import de.sebastianhesse.pbf.reader.Accessor;
import de.sebastianhesse.pbf.routing.calculators.CalculationType;


/**
 *
 */
public class DijkstraOptions {

    private Accessor accessor;
    private CalculationType calculationType;


    public DijkstraOptions(Accessor accessor, CalculationType calculationType) {
        this.accessor = accessor;
        this.calculationType = calculationType;
    }


    public static DijkstraOptions shortestWithCar() {
        return new DijkstraOptions(Accessor.CAR, CalculationType.SHORTEST);
    }


    public static DijkstraOptions shortestWithPedestrian() {
        return new DijkstraOptions(Accessor.PEDESTRIAN, CalculationType.SHORTEST);
    }


    public static DijkstraOptions fastestWithCar() {
        return new DijkstraOptions(Accessor.CAR, CalculationType.FASTEST);
    }


    public static DijkstraOptions fastestWithPedestrian() {
        return new DijkstraOptions(Accessor.PEDESTRIAN, CalculationType.FASTEST);
    }


    public Accessor getAccessor() {
        return accessor;
    }


    public CalculationType getCalculationType() {
        return calculationType;
    }
}
