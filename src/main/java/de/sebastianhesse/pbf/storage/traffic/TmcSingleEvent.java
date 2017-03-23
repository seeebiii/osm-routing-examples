package de.sebastianhesse.pbf.storage.traffic;

/**
 * This is more like a marker class to differentiate {@link TmcEvent}s.
 */
public class TmcSingleEvent extends TmcEvent {

    public TmcSingleEvent(String row, int eventCode, int locationCode, short extend, boolean direction) {
        super(row, eventCode, locationCode, extend, direction);
    }


}
