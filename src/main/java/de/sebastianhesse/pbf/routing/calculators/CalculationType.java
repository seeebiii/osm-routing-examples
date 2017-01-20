package de.sebastianhesse.pbf.routing.calculators;

import de.sebastianhesse.pbf.routing.Dijkstra;


/**
 * Used to describe the {@link Dijkstra} implementation. Either shortest way (i.e. search for distance minimum) or
 * fastest way (i.e. way where vehicle can travel the fastest).
 */
public enum CalculationType {

    SHORTEST,
    FASTEST
}
