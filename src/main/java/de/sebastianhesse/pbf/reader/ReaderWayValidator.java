package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderWay;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A custom validator for graph data.
 */
public class ReaderWayValidator {

    protected static Set<String> notAcceptedTags = new HashSet<>();
    protected static Map<String, Short> maxSpeeds = new HashMap<>();
    protected static Set<String> positiveRestrictions = new HashSet<>();


    public ReaderWayValidator() {
        notAcceptedTags.add("barrier");
        notAcceptedTags.add("amenity");
        notAcceptedTags.add("leisure");
        notAcceptedTags.add("sports");
        notAcceptedTags.add("tourism");
        notAcceptedTags.add("landuse");

        // Types from http://wiki.openstreetmap.org/wiki/Key:highway
        maxSpeeds.put("motorway", (short) 100);
        maxSpeeds.put("motorroad", (short) 90);
        maxSpeeds.put("trunk", (short) 70);
        maxSpeeds.put("primary", (short) 65);
        maxSpeeds.put("secondary", (short) 60);
        maxSpeeds.put("tertiary", (short) 50);
        maxSpeeds.put("unclassified", (short) 30);
        maxSpeeds.put("residential", (short) 30);
        maxSpeeds.put("service", (short) 20);
        maxSpeeds.put("motorway_link", (short) 70);
        maxSpeeds.put("trunk_link", (short) 65);
        maxSpeeds.put("primary_link", (short) 60);
        maxSpeeds.put("secondary_link", (short) 50);
        maxSpeeds.put("tertiary_link", (short) 40);
        maxSpeeds.put("living_street", (short) 5);
        maxSpeeds.put("road", (short) 20);
        maxSpeeds.put("track", (short) 15);

        positiveRestrictions.add("yes");
        positiveRestrictions.add("unknown");
        positiveRestrictions.add("permissive");
        positiveRestrictions.add("destination");
    }


    public boolean isValidWay(ReaderWay way) {
        for (String notAcceptedTag : notAcceptedTags) {
            if (StringUtils.isNotBlank(way.getTag(notAcceptedTag))) {
                return false;
            }
        }

        return way.getNodes().size() > 1 && !(way.hasTag("impassable", "yes") || way.hasTag("status", "impassable") ||
                way.hasTag("area", "yes")) && StringUtils.isBlank(way.getTag("leisure")) &&
                maxSpeeds.containsKey(way.getTag("highway"));
    }


    public boolean isNotOneWay(Object way) {
        if (way instanceof ReaderWay) {
            return !isOneWay((ReaderWay) way);
        } else {
            return way instanceof Way && !((Way) way).isOneWay();
        }
    }


    public boolean isOneWay(ReaderWay way) {
        return way.hasTag("oneway", "yes", "-1") ||
                way.hasTag("highway", "motorway", "motorway_link", "trunk_link", "primary_link") ||
                way.hasTag("junction", "roundabout");
    }


    public short getMaxSpeed(ReaderWay way) {
        String maxspeed = way.getTag("maxspeed");
        try {
            return Short.valueOf(maxspeed);
        } catch (NumberFormatException ex) {
            if ("none".equals(maxspeed)) {
                // max speed
                return 130;
            } else {
                // try to retrieve by highway type
                String highwayType = way.getTag("highway");
                if (highwayType == null) {
                    return 1;
                }

                Short speed = maxSpeeds.get(highwayType);
                if (speed == null) {
                    return 0;
                } else {
                    return speed;
                }
            }
        }
    }


    public boolean hasAccess(ReaderWay way, Accessor accessor) {
        if (!way.hasTag("access") && !way.hasTag("motorcar") && !way.hasTag("motor_vehicle")) {
            return true;
        } else {
            switch (accessor) {
                case CAR:
                    return way.hasTag("motorcar", positiveRestrictions) ||
                            way.hasTag("motor_vehicle", positiveRestrictions) ||
                            way.hasTag("access", positiveRestrictions);
                case PEDESTRIAN:
                    return way.hasTag("foot", positiveRestrictions);
                default:
                    return false;
            }
        }
    }
}
