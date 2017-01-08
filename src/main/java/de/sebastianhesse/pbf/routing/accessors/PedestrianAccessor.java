package de.sebastianhesse.pbf.routing.accessors;

import de.sebastianhesse.pbf.storage.Edge;


/**
 *
 */
public class PedestrianAccessor implements WayAccessor {

    @Override
    public boolean canAccessWay(Edge edge) {
        return edge.isPedestrianAllowed();
    }


    @Override
    public short getMaxSpeed() {
        return 7;
    }
}
