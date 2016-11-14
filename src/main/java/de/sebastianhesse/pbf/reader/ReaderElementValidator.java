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


    public ReaderElementValidator() {
        notAcceptedTags.add("barrier");
        notAcceptedTags.add("amenity");
    }


    public boolean isValidWay(ReaderWay way) {
        boolean isValid = true;
        for (String notAcceptedTag : this.notAcceptedTags) {
            isValid = StringUtils.isBlank(way.getTag(notAcceptedTag));
            if (!isValid) {
                return false;
            }
        }

        // at this point isValid is always true, otherwise the method would have returned false before
        return way.getNodes().size() > 1 && !(way.hasTag("impassable", "yes") || way.hasTag("status", "impassable") ||
                way.hasTag("area", "yes") || way.hasTag("highway", "raceway"));
    }


    public boolean isNotOneWay(ReaderWay way) {
        return !way.hasTag("oneway", "yes", "-1");
    }
}
