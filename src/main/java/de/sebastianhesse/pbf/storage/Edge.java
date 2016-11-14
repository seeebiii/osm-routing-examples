package de.sebastianhesse.pbf.storage;

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

        if (sourceNode != edge.sourceNode) return false;
        return targetNode == edge.targetNode;

    }


    @Override
    public int hashCode() {
        int result = (int) (sourceNode ^ (sourceNode >>> 32));
        result = 31 * result + (int) (targetNode ^ (targetNode >>> 32));
        return result;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sourceNode", sourceNode)
                .append("targetNode", targetNode)
                .toString();
    }
}
