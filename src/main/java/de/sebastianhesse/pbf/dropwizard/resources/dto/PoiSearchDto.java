package de.sebastianhesse.pbf.dropwizard.resources.dto;

import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.util.GraphUtil;

import java.util.List;
import java.util.stream.Collectors;


/**
 * A DTO containing the start point and the requested POIs around the start point.
 */
public class PoiSearchDto {

    public Node startPoint;
    public List<Double[]> poiList;


    public PoiSearchDto(Node startPoint, List<Node> nodes) {
        this.startPoint = startPoint;
        this.poiList = nodes.stream()
                .distinct()
                .map(node -> {
                    double id = Long.valueOf(node.getId()).doubleValue();
                    double distance = GraphUtil.getDistance(startPoint, node);
                    // always make sure that lat is the first and lon the second (so it's easier to handle in the frontend)
                    return new Double[]{node.getLat(), node.getLon(), id, distance};
                })
                .sorted((o1, o2) -> {
                    if (o1 == null || o2 == null) {
                        return 0;
                    }
                    // compare distances and sort by distance ASC
                    if (o1[3] > o2[3]) {
                        return 1;
                    } else if (o1[3] < o2[3]) {
                        return -1;
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }
}
