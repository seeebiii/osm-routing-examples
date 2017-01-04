package de.sebastianhesse.pbf.reader;

import de.sebastianhesse.pbf.storage.Graph;


/**
 *
 */
public interface NodeEdgeReader {

    NodeEdgeReader importData() throws Exception;

    Graph getGraph();
}
