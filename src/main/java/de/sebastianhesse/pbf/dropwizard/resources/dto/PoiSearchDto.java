package de.sebastianhesse.pbf.dropwizard.resources.dto;

import de.sebastianhesse.pbf.storage.Node;


/**
 * A DTO containing the start point and the requested POIs around the start point.
 */
public class PoiSearchDto {

    public Node startPoint;
    public SingleRouteDto poiList;


    public PoiSearchDto(Node startPoint, SingleRouteDto poiList) {
        this.startPoint = startPoint;
        this.poiList = poiList;
    }
}
