package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Stores the boundaries of a graph so you can build a coordinate system around all OSM data:
 * upperLat/lowerLon ---------- upperLat/upperLon
 *         |                            |
 *         |                            |
 *         |                            |
 *         |                            |
 * lowerLat/lowerLon ---------- lowerLat/upperLon
 * A boundary corner must not be an existing point of [lat,lon] in the graph data.
 */
public class GraphBoundary {

    private double upperLat = Double.MIN_VALUE;
    private double upperLon = Double.MIN_VALUE;
    private double lowerLat = Double.MAX_VALUE;
    private double lowerLon = Double.MAX_VALUE;

    public GraphBoundary() {
    }


    public void updateCorners(Node node) {
        if (node != null) {
            if (upperLat < node.getLat()) {
                upperLat = node.getLat();
            }

            if (upperLon < node.getLon()) {
                upperLon = node.getLon();
            }

            if (lowerLat > node.getLat()) {
                lowerLat = node.getLat();
            }

            if (lowerLon > node.getLon()) {
                lowerLon = node.getLon();
            }
        }
    }


    public double getUpperLat() {
        return upperLat;
    }


    public double getUpperLon() {
        return upperLon;
    }


    public double getLowerLat() {
        return lowerLat;
    }


    public double getLowerLon() {
        return lowerLon;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("upperLat", upperLat)
                .append("upperLon", upperLon)
                .append("lowerLat", lowerLat)
                .append("lowerLon", lowerLon)
                .toString();
    }


    public Node[][] getBoundaryNodes() {
        Node[] north = new Node[]{new Node(this.upperLat, this.lowerLon), new Node(this.upperLat, this.upperLon)};
        Node[] south = new Node[]{new Node(this.lowerLat, this.lowerLon), new Node(this.lowerLat, this.upperLon)};
        return new Node[][]{north, south};
    }
}
