package de.sebastianhesse.pbf.dropwizard.resources.dto;

import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.util.GraphUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * DTO to return list of points for a certain path calculated by {@link de.sebastianhesse.pbf.routing.Dijkstra}.
 */
public class LatLngList {

    public List<Double[]> points;
    public double distance = 0;


    public LatLngList(List<Node> nodes) {
        this.points = nodes.stream()
                .map(node -> new Double[] {node.getLat(), node.getLon(), Long.valueOf(node.getId()).doubleValue()})
                .collect(Collectors.toCollection(ArrayList::new));

        if (this.points.size() >= 2) {
            Double[] last = this.points.get(0);
            for (int i = 1; i < this.points.size(); i++) {
                Double[] current = this.points.get(i);
                this.distance += GraphUtil.getDistance(new Node(last[0], last[1]), new Node(current[0], current[1]));
                last = current;
            }
        }
    }
}
