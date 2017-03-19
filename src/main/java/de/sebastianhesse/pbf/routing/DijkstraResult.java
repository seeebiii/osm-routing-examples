package de.sebastianhesse.pbf.routing;

import de.sebastianhesse.pbf.storage.Node;

import java.util.List;


/**
 *
 */
public class DijkstraResult {

    public List<Node> path;
    public double distance;
    public double timeInSeconds;


    public DijkstraResult(List<Node> path, double distance, double timeInSeconds) {
        this.path = path;
        this.distance = distance;
        this.timeInSeconds = timeInSeconds;
    }
}
