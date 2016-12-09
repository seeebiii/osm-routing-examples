package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderWay;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;


/**
 * A custom validator for graph data.
 */
public class ReaderElementValidator {

    protected Set<String> notAcceptedTags = new HashSet<>();
    protected Set<String> notAcceptedHighwayValues = new HashSet<>();


    public ReaderElementValidator() {
        notAcceptedTags.add("barrier");
        notAcceptedTags.add("amenity");
        notAcceptedTags.add("leisure");
        notAcceptedTags.add("sports");
        notAcceptedTags.add("tourism");
        notAcceptedTags.add("landuse");

        notAcceptedHighwayValues.add("raceway");
        notAcceptedHighwayValues.add("escape");
        notAcceptedHighwayValues.add("bus_guideway");
        notAcceptedHighwayValues.add("footway");
        notAcceptedHighwayValues.add("bridleway");
        notAcceptedHighwayValues.add("steps");
        notAcceptedHighwayValues.add("path");
        notAcceptedHighwayValues.add("cycleway");
        notAcceptedHighwayValues.add("proposed");
    }


    public boolean isValidWay(ReaderWay way) {
        for (String notAcceptedTag : this.notAcceptedTags) {
            if (StringUtils.isNotBlank(way.getTag(notAcceptedTag))) {
                return false;
            }
        }

        return way.getNodes().size() > 1 && !(way.hasTag("impassable", "yes") || way.hasTag("status", "impassable") ||
                way.hasTag("area", "yes")) && StringUtils.isBlank(way.getTag("leisure")) &&
                !way.hasTag("highway", notAcceptedHighwayValues);
    }


    public boolean isNotOneWay(Object way) {
        if (way instanceof ReaderWay) {
            return !isOneWay((ReaderWay) way);
        } else {
            return way instanceof Way && !((Way) way).isOneWay();
        }
    }


    public boolean isOneWay(ReaderWay way) {
        return way.hasTag("oneway", "yes", "-1");
    }
}
