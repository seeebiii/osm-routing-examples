package de.sebastianhesse.pbf.storage;

/**
 * Offset class to be used for a {@link Graph} to structure objects within an array using a certain ordering.
 * Indicates the starting offset and a link to the next offset to simplify searching.
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
