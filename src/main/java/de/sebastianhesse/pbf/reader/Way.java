package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderWay;
import gnu.trove.list.TLongList;


/**
 * Class to simplify a way of an OSM file.
 */
public class Way {

    private TLongList nodes;
    private String type;
    private boolean isOneWay;
    private short maxSpeed = 0;

    public Way(ReaderWay readerWay, boolean isOneWay, short maxSpeed) {
        this.nodes = readerWay.getNodes();
        this.type = readerWay.getTag("highway");
        this.isOneWay = isOneWay;
        this.maxSpeed = maxSpeed;
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
}
