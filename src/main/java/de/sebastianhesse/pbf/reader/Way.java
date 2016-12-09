package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderWay;
import gnu.trove.list.TLongList;


/**
 * Class to simplify a way of an OSM file.
 */
public class Way {

    private TLongList nodes;
    private boolean isOneWay;

    public Way(ReaderWay readerWay, boolean isOneWay) {
        this.nodes = readerWay.getNodes();
        this.isOneWay = isOneWay;
    }


    public TLongList getNodes() {
        return nodes;
    }


    public boolean isOneWay() {
        return isOneWay;
    }
}
