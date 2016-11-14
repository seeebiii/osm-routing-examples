package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * A {@link Graph} node containing latitude and longitude information. Also holds an offset pointer with the index
 * of the nodes first {@link Edge} within the edge array of a {@link Graph}. This is used for routing.
 */
public class Node {

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

        if (Double.compare(node.lat, lat) != 0) return false;
        if (Double.compare(node.lon, lon) != 0) return false;
        return offsetPointer == node.offsetPointer;

    }


    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + offsetPointer;
        return result;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("lat", lat)
                .append("lon", lon)
                .append("offsetPointer", offsetPointer)
                .toString();
    }
}
