package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Connection between a source and a target node. Has some certain properties like allowed speed,
 * distance (from source to target) or access rights for different vehicles.
 */
public class Edge {

    /**
     * highway type of the edge, e.g. motorway_link
     */
    private String type;
    private int sourceNode;
    private int targetNode;
    private int nextCrossing;
    private double distance = 0;
    private short speed = 0;
    // allowed: car, pedestrian
    private boolean[] access = new boolean[] {false, false};


    public Edge(String type, int sourceNode, int targetNode) {
        this.type = type;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.nextCrossing = -1;
    }


    public String getType() {
        return type;
    }


    public int getSourceNode() {
        return sourceNode;
    }


    public int getTargetNode() {
        return targetNode;
    }


    public void setNextCrossing(int crossing) {
        this.nextCrossing = crossing;
    }


    public int getNextCrossing() {
        return this.nextCrossing;
    }


    public double getDistance() {
        return distance;
    }


    public void setDistance(double distance) {
        this.distance = distance;
    }


    public short getSpeed() {
        return speed;
    }


    public void setSpeed(short speed) {
        this.speed = speed;
    }


    public void setAccess(boolean[] access) {
        this.access = access;
    }


    public boolean isCarAllowed() {
        return this.access[0];
    }


    public boolean isPedestrianAllowed() {
        return this.access[1];
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Edge edge = (Edge) o;

        return new EqualsBuilder()
                .append(sourceNode, edge.sourceNode)
                .append(targetNode, edge.targetNode)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(sourceNode)
                .append(targetNode)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sourceNode", sourceNode)
                .append("targetNode", targetNode)
                .toString();
    }
}
