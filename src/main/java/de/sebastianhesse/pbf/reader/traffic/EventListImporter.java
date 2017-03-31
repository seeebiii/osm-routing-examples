package de.sebastianhesse.pbf.reader.traffic;

import de.sebastianhesse.pbf.storage.traffic.EventList;
import de.sebastianhesse.pbf.storage.traffic.EventRow;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


/**
 * An importer for an event list.
 */
public class EventListImporter {

    private static final Logger logger = LoggerFactory.getLogger(EventListImporter.class);
    public static final String CSV_SEPARATOR = ";";

    private File eventListFile;


    public EventListImporter(String path) {
        if (StringUtils.isNotBlank(path)) {
            this.eventListFile = new File(path);
        }
    }


    /**
     * Imports all relevant entries from {@link #eventListFile}. An entries must have a code and a text.
     *
     * @return {@link EventList} contain all relevant entries
     */
    public EventList importEventList() {
        EventList list = new EventList();
        if (this.eventListFile == null || !this.eventListFile.exists()) {
            logger.warn("File is not set or does not exist, hence skipping import.");
            return list;
        }

        try {
            List<String> rows = IOUtils.readLines(new FileInputStream(this.eventListFile), Charsets.UTF_8);
            // filter out description row
            rows = rows.subList(1, rows.size());
            for (String row : rows) {
                String[] splitRow = StringUtils.splitPreserveAllTokens(row, CSV_SEPARATOR);
                Optional<EventRow> eventRow = getEventRow(splitRow);
                eventRow.ifPresent(list::add);
            }
            logger.debug("Successfully imported EventList.");

            return list;
        } catch (IOException e) {
            logger.error("Could not read file for EventList.", e);
        }
        return list;
    }


    private Optional<EventRow> getEventRow(String[] splitRow) {
        if (splitRow.length < 7) {
            return Optional.empty();
        }
        String id = splitRow[6];
        String text = splitRow[1];
        if (StringUtils.isNoneBlank(id, text)) {
            // text without quantifier
            String textDe = splitRow[3];
            return Optional.of(new EventRow(Integer.valueOf(id), text, textDe, getWeight(text)));
        }
        return Optional.empty();
    }


    private double getWeight(String text) {
        text = text.toLowerCase();
        if (StringUtils.containsAny(text, "bridge closed", "bridge blocked", "tunnel closed", "tunnel blocked",
                "both directions closed", "closed due to ")) {
            return 1;
        } else if (StringUtils.contains(text, "stationary traffic")) {
            return 0.85;
        } else if (StringUtils.contains(text, "queuing traffic")) {
            return 0.8;
        } else if (StringUtils.contains(text, "heavy traffic")) {
            return 0.5;
        } else if (StringUtils.contains(text, "slow traffic")) {
            return 0.3;
        } else if (StringUtils.containsAny(text, "construction work", "construction traffic merging", "roadwork",
                "resurfacing work")) {
            return 0.6;
        } else if (StringUtils.containsAny(text, "traffic problem", "traffic congestion")) {
            return 0.1;
        } else if (StringUtils.contains(text, "accident")) {
            return 0.75;
        } else if (StringUtils.containsAny(text, "lane closed", "lanes closed", "lane blocked", "lanes blocked",
                "lane(s) blocked", "blocked by")) {
            return 0.6;
        } else if (StringUtils.contains(text, "passable with care")) {
            return 0.2;
        } else if (StringUtils.contains(text, "delays")) {
            return 0.5;
        } else if (StringUtils.containsAny(text, "object(s) on the road", "people on roadway", "broken down vehicle",
                "animals on the road")) {
            return 0.2;
        } else if (StringUtils.containsAny(text, "maintenance work", "central reservation work")) {
            return 0.3;
        } else {
            return 0;
        }
    }
}
