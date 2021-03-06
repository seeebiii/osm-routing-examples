package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * A {@link Graph} node containing latitude and longitude information. Also holds an offset pointer with the index
 * of the nodes first {@link Edge} within the edge array of a {@link Graph}. This is used for routing.
 */
public class Node implements Comparable<Node> {

    private long id = -1;
    private double lat;
    private double lon;
    private int offsetPointer = -1;
    /**
     * if this node is a junction or node
     */
    private boolean crossing = false;
    /**
     * if this node is a POI or not
     */
    private boolean poi = false;
    /**
     * type of POI
     */
    private String typeKey = "";
    /**
     * value for POI type
     */
    private String typeValue = "";


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


    public boolean isCrossing() {
        return crossing;
    }


    public void setCrossing(boolean crossing) {
        this.crossing = crossing;
    }


    public boolean isPoi() {
        return poi;
    }


    public void setPoi(boolean poi) {
        this.poi = poi;
    }


    public void setType(String key, String value) {
        this.typeKey = key;
        this.typeValue = value;
    }


    public boolean isTypeOf(String key, String value) {
        return StringUtils.equalsIgnoreCase(this.typeKey, key) && StringUtils.equalsIgnoreCase(this.typeValue, value);
    }


    public String getTypeKey() {
        return typeKey;
    }


    public String getTypeValue() {
        return typeValue;
    }


    @Override
    public int compareTo(Node o) {
        if (o == null) {
            return 1;
        }
        return (int) (this.getId() - o.getId());
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
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(lat)
                .append(lon)
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
