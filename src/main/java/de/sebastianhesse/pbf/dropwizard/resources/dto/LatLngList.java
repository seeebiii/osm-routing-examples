package de.sebastianhesse.pbf.dropwizard.resources.dto;

import de.sebastianhesse.pbf.storage.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * DTO to return list of points for a certain path calculated by {@link de.sebastianhesse.pbf.routing.Dijkstra}.
 */
public class LatLngList {

    public List<Double[]> points;


    public LatLngList(List<Node> nodes) {
        this.points = nodes.stream()
                .map(node -> new Double[] {node.getLat(), node.getLon()})
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
