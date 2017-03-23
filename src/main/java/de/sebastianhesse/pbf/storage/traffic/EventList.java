package de.sebastianhesse.pbf.storage.traffic;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * Container to hold all types of events which are represented by {@link EventRow}.
 */
public class EventList {


    private TIntObjectMap<EventRow> eventList;
    private int rows = 0;


    public EventList() {
        this.eventList = new TIntObjectHashMap<>();
    }


    public void add(EventRow row) {
        this.eventList.put(row.getId(), row);
        this.rows++;
    }


    public EventRow get(int id) {
        if (this.eventList.containsKey(id)) {
            return this.eventList.get(id);
        }
        throw new IllegalArgumentException("EventList does not contain an element with id=" + id);
    }


    public int size() {
        return this.rows;
    }
}
