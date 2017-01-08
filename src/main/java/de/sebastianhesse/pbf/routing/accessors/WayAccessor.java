package de.sebastianhesse.pbf.routing.accessors;

import de.sebastianhesse.pbf.storage.Edge;


/**
 *
 */
public interface WayAccessor {

    boolean canAccessWay(Edge edge);

    short getMaxSpeed();
}
