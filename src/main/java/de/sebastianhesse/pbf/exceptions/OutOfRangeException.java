package de.sebastianhesse.pbf.exceptions;

/**
 * Indicates that a lat/lon was requested which does not match the range of imported OSM data.
 */
public class OutOfRangeException extends RuntimeException {

    public OutOfRangeException(String message) {
        super(message);
    }
}
