package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderWay;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;


/**
 * Class to simplify a way of an OSM file.
 */
public class Way {

    private TLongList nodes;
    private String type;
    private boolean isOneWay;
    private short maxSpeed = 0;
    // car, pedestrian
    private boolean[] access;


    private Way(WayBuilder builder) {
        this.nodes = builder.nodes;
        this.type = builder.type;
        this.isOneWay = builder.isOneWay;
        this.access = new boolean[]{builder.hasCarAccess, builder.hasPedestrianAccess};
        this.maxSpeed = builder.maxSpeed;
    }


    public TLongList getNodes() {
        return nodes;
    }


    public boolean isOneWay() {
        return isOneWay;
    }


    public short getMaxSpeed() {
        return this.maxSpeed;
    }


    public String getType() {
        return this.type;
    }


    public boolean[] getAccess() {
        return this.access;
    }


    public static final class WayBuilder {

        ReaderWayValidator validator = new ReaderWayValidator();

        TLongList nodes = new TLongArrayList();
        String type = "";
        boolean isOneWay = false;
        short maxSpeed = 0;
        boolean hasCarAccess = true;
        boolean hasPedestrianAccess = true;


        public WayBuilder() {
        }


        public WayBuilder setOriginalWay(ReaderWay readerWay) {
            this.nodes = readerWay.getNodes();
            this.type = readerWay.getTag("highway");
            this.isOneWay = this.validator.isOneWay(readerWay);
            this.maxSpeed = this.validator.getMaxSpeed(readerWay);
            this.hasCarAccess = this.validator.hasAccess(readerWay, Accessor.CAR);
            this.hasPedestrianAccess = this.validator.hasAccess(readerWay, Accessor.PEDESTRIAN);
            return this;
        }


        public Way build() {
            return new Way(this);
        }
    }
}
