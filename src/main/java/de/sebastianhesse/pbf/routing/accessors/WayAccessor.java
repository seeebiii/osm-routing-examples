package de.sebastianhesse.pbf.routing.accessors;

import de.sebastianhesse.pbf.storage.Edge;


/**
 * Interface for different vehicles types which can be used for routing. A Dijkstra implementation can use an
 * implementation to get information like access rights for a certain vehicle.
 */
public interface WayAccessor {

    /**
     * @param edge an edge from a node to a neighbour
     * @return true if the vehicle can access the way
     */
    boolean canAccessWay(Edge edge);


    /**
     * @return max. speed for vehicle
     */
    short getMaxSpeed();
}
