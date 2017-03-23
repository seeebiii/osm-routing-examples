package de.sebastianhesse.pbf.dropwizard.resources.dto;

import de.sebastianhesse.pbf.storage.Node;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A DTO class to serve different point-to-point ways. Point-to-point => always the first and second are a pair
 * and form a way. (Bad structure, but efficient)
 */
public class TrafficWaysDto {

    public short hour;
    public List<Double[]> points;


    public TrafficWaysDto(short hour, List<Pair<Node, Node>> nodes) {
        this.hour = hour;
        this.points = nodes.stream()
                .flatMap(nodePair -> {
                    List<Double[]> list = new ArrayList<>(2);
                    Node key = nodePair.getKey();
                    list.add(new Double[]{key.getLat(), key.getLon(), Long.valueOf(key.getId()).doubleValue()});
                    Node value = nodePair.getValue();
                    list.add(new Double[]{value.getLat(), value.getLon(), Long.valueOf(value.getId()).doubleValue()});
                    return list.stream();
                })
                .collect(Collectors.toList());
    }
}
