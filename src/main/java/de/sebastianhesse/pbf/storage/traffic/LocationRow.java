package de.sebastianhesse.pbf.storage.traffic;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;


/**
 * A class holding one set of information of the Location Code List.
 */
public class LocationRow {

    /**
     * The location identifier.
     */
    private int id;
    /**
     * The type of the location, e.g. a street, a place, etc.
     */
    private String type;
    /**
     * The id of another {@link LocationRow}.
     */
    private int negOffset;
    /**
     * The id of another {@link LocationRow}.
     */
    private int posOffset;
    /**
     * The latitude parameter of the geographical location of this instance.
     */
    private double lat;
    /**
     * The longitude parameter of the geographical location of this instance.
     */
    private double lon;


    public LocationRow(int id, String type, int negOffset, int posOffset, double lat, double lon) {
        this.id = id;
        this.type = type;
        this.negOffset = negOffset;
        this.posOffset = posOffset;
        this.lat = lat;
        this.lon = lon;
    }


    public static LocationRowBuilder builder(String id, String type) {
        return new LocationRowBuilder(id, type);
    }


    public int getId() {
        return id;
    }


    public String getType() {
        return type;
    }


    public double getLat() {
        return lat;
    }


    public double getLon() {
        return lon;
    }


    public int getNegOffset() {
        return negOffset;
    }


    public int getPosOffset() {
        return posOffset;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        LocationRow that = (LocationRow) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("type", type)
                .append("negOffset", negOffset)
                .append("posOffset", posOffset)
                .append("lat", lat)
                .append("lon", lon)
                .toString();
    }


    public static class LocationRowBuilder {

        private int id;
        private String type;
        private int negOffset = -1;
        private int posOffset = -1;
        private double lat = 0;
        private double lon = 0;

        private LocationRowBuilder(String id, String type) {
            if (StringUtils.isAnyBlank(id, type)) {
                throw new IllegalArgumentException("ID and type must be set for a LocationRow.");
            }
            this.id = Integer.valueOf(id);
            this.type = type;
        }


        public LocationRowBuilder addOffsets(String negOffset, String posOffset) {
            if (StringUtils.isNoneBlank(negOffset, posOffset)) {
                this.negOffset = Integer.valueOf(negOffset);
                this.posOffset = Integer.valueOf(posOffset);
            }
            return this;
        }


        public LocationRowBuilder addLatLon(String lat, String lon) {
            // make sure to import the numbers in German format!
            if (StringUtils.isNoneBlank(lat, lon)) {
                try {
                    this.lat = DecimalFormat.getNumberInstance(Locale.GERMANY).parse(lat).doubleValue();
                    this.lon = NumberFormat.getNumberInstance(Locale.GERMANY).parse(lon).doubleValue();
                } catch (ParseException e) {
                    try {
                        this.lat = Double.valueOf(lat);
                        this.lon = Double.valueOf(lon);
                    } catch (NumberFormatException e1) {
                        // can not parse lat and lon
                    }
                }
            }
            return this;
        }


        public LocationRow build() {
            return new LocationRow(this.id, this.type, this.negOffset, this.posOffset, this.lat, this.lon);
        }
    }
}
