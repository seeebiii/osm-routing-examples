package de.sebastianhesse.pbf.routing.accessors;

import de.sebastianhesse.pbf.storage.Edge;


/**
 * Accessor for vehicle type 'car'.
 */
public class CarAccessor implements WayAccessor {

    @Override
    public boolean canAccessWay(Edge edge) {
        return edge.isCarAllowed();
    }


    @Override
    public short getMaxSpeed() {
        return 130;
    }
}
