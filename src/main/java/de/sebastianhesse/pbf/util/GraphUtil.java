package de.sebastianhesse.pbf.util;

import de.sebastianhesse.pbf.storage.Node;


/**
 * Utility class for Graphs.
 */
public class GraphUtil {

    public static double getDistance(Node node, Node target) {
        double R = 6372800; // metres
        double lat1 = node.getLat();
        double lat2 = target.getLat();
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(target.getLon() - node.getLon());
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}
