package de.sebastianhesse.pbf.reader;

import de.sebastianhesse.pbf.storage.Graph;


/**
 * A NodeEdgeReader is capable of reading OSM data and converting it into a {@link Graph} structure.
 */
public interface NodeEdgeReader {

    /**
     * Imports data, e.g. from a file containing OSM data. The data source must be handled by the implementation.
     * @return an instance of the implementation containing the imported data
     * @throws Exception if an error occurs, e.g. cannot read file, malformed data, etc.
     */
    NodeEdgeReader importData() throws Exception;

    /**
     * @return a graph containing the imported data, i.e. nodes and edges
     */
    Graph getGraph();
}
