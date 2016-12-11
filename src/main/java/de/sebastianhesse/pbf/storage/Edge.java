package de.sebastianhesse.pbf.storage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 *
 */
public class Edge {

    private int sourceNode;
    private int targetNode;


    public Edge(int sourceNode, int targetNode) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }


    public int getSourceNode() {
        return sourceNode;
    }


    public int getTargetNode() {
        return targetNode;
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
