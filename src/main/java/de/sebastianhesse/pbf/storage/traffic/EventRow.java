package de.sebastianhesse.pbf.storage.traffic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Holds information about traffic related events, e.g. roadworks or a crash. Instances of this class should
 * have a {@link #weight} between 0 and 1.0 (both inclusive).
 */
public class EventRow {

    /**
     * The event id to be recognized in TMC events.
     */
    private int id;
    /**
     * The original event description in English.
     */
    private String text;
    /**
     * The original event description in German.
     */
    private String textDe;
    /**
     * A weight indicating the size of the resulting delay, e.g. a factor about how much more time needs is needed
     * for a fastest way calculation.
     */
    private double weight;


    public EventRow(int id, String text, String textDe, double weight) {
        this.id = id;
        this.text = text;
        this.textDe = textDe;
        this.weight = weight;
    }


    public int getId() {
        return id;
    }


    public String getText() {
        return text;
    }


    public String getTextDe() {
        return textDe;
    }


    public double getWeight() {
        return weight;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EventRow eventRow = (EventRow) o;

        return new EqualsBuilder()
                .append(id, eventRow.id)
                .append(weight, eventRow.weight)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(weight)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("text", text)
                .append("textDe", textDe)
                .append("weight", weight)
                .toString();
    }
}
