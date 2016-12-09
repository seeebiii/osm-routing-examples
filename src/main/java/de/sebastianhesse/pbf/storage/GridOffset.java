package de.sebastianhesse.pbf.storage;

/**
 *
 */
public class GridOffset {

    private String name;
    private int offset;
    private String nextOffset;


    public GridOffset(String name, int offset) {
        this.name = name;
        this.offset = offset;
        this.nextOffset = "";
    }


    public void setNextOffset(String nextOffset) {
        this.nextOffset = nextOffset;
    }


    public String getName() {
        return name;
    }


    public int getOffset() {
        return offset;
    }


    public String getNextOffset() {
        return nextOffset;
    }
}
