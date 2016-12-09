package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderElement;


/**
 * Interface to handle a {@link ReaderElement} of an OSM file.
 */
@FunctionalInterface
public interface ItemHandler {

    boolean handle(ReaderElement item);
}