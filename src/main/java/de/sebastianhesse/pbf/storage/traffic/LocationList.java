package de.sebastianhesse.pbf.storage.traffic;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * A container holding data about the relevant Location Code List. The German list contains about 50k entries.
 */
public class LocationList {

    private TIntObjectMap<LocationRow> locations;
    private int rows = 0;


    public LocationList() {
        locations = new TIntObjectHashMap<>(67000);
    }


    public void add(LocationRow row) {
        this.locations.put(row.getId(), row);
        this.rows++;
    }


    public LocationRow get(int id) {
        if (this.locations.containsKey(id)) {
            return this.locations.get(id);
        }
        throw new IllegalArgumentException("LocationList does not contain an element with id=" + id);
    }


    public int size() {
        return this.rows;
    }

}
