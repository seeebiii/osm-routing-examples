package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * A {@link Graph} node containing latitude and longitude information. Also holds an offset pointer with the index
 * of the nodes first {@link Edge} within the edge array of a {@link Graph}. This is used for routing.
 */
public class Node {

    private long id;
    private double lat;
    private double lon;
    private int offsetPointer = -1;


    public Node(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }


    public double getLat() {
        return lat;
    }


    public double getLon() {
        return lon;
    }


    public long getId() {
        return id;
    }


    public void setId(long id) {
        this.id = id;
    }


    public int getOffsetPointer() {
        return offsetPointer;
    }


    public void setOffsetPointer(int offsetPointer) {
        this.offsetPointer = offsetPointer;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return new EqualsBuilder()
                .append(id, node.id)
                .append(lat, node.lat)
                .append(lon, node.lon)
                .append(offsetPointer, node.offsetPointer)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(lat)
                .append(lon)
                .append(offsetPointer)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("lat", lat)
                .append("lon", lon)
                .append("offsetPointer", offsetPointer)
                .toString();
    }
}
