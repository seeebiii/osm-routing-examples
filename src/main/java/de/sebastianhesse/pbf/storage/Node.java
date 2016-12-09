package de.sebastianhesse.pbf.storage;

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



}
