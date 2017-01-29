package de.sebastianhesse.pbf.dropwizard.resources.dto;

/**
 * DTO to transport meta information.
 */
public class MetaDto {

    public String osmFile;
    public String importStrategy;
    public int nodes;
    public int edges;


    public MetaDto(String osmFile, String importStrategy, int nodes, int edges) {
        this.osmFile = osmFile;
        this.importStrategy = importStrategy;
        this.nodes = nodes;
        this.edges = edges;
    }
}
