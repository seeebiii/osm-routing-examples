package de.sebastianhesse.pbf.storage.traffic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Parent container to hold the data for a TMC event. The data must be connected to some other sources,
 * i.e. a list of event types and a location code list.
 */
public class TmcEvent {

    /**
     * The original event row from TMC data.
     */
    private String row;
    /**
     * The code to identify the related event.
     */
    private int eventCode;
    /**
     * The code to identify the related location.
     */
    private int locationCode;
    /**
     * The number of extensions to the related location.
     */
    private short extend;
    /**
     * Indicates negative or positive offset for location. True=1 => negative offset, False=0 => positive offset.
     */
    private boolean direction;


    public TmcEvent(String row, int eventCode, int locationCode, short extend, boolean direction) {
        this.row = row;
        this.eventCode = eventCode;
        this.locationCode = locationCode;
        this.extend = extend;
        this.direction = direction;
    }


    public String getRow() {
        return row;
    }


    public int getEventCode() {
        return eventCode;
    }


    public int getLocationCode() {
        return locationCode;
    }


    public short getExtend() {
        return extend;
    }


    public boolean isDirection() {
        return direction;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TmcEvent event = (TmcEvent) o;

        return new EqualsBuilder()
                .append(eventCode, event.eventCode)
                .append(locationCode, event.locationCode)
                .append(extend, event.extend)
                .append(direction, event.direction)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(eventCode)
                .append(locationCode)
                .append(extend)
                .append(direction)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("eventCode", eventCode)
                .append("locationCode", locationCode)
                .append("extend", extend)
                .append("direction", direction)
                .toString();
    }
}
