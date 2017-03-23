package de.sebastianhesse.pbf.reader.traffic;

import de.sebastianhesse.pbf.storage.traffic.LocationList;
import de.sebastianhesse.pbf.storage.traffic.LocationRow;
import org.apache.commons.compress.utils.Charsets;
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
 * An importer for a Location Code List as a CSV file.
 */
public class LocationListImporter {

    private static final Logger logger = LoggerFactory.getLogger(LocationListImporter.class);

    public static final String CSV_SEPARATOR = ";";
    private File locationListPath;


    public LocationListImporter(String locationListPath) {
        if (StringUtils.isNotBlank(locationListPath)) {
            this.locationListPath = new File(locationListPath);
        }
    }


    /**
     * Imports all relevant entries from {@link #locationListPath}.
     *
     * @return a {@link LocationList} containing all entries
     */
    public LocationList importLocationList() {
        LocationList list = new LocationList();
        if (this.locationListPath == null || !this.locationListPath.exists()) {
            logger.warn("File is not set or does not exist, hence skipping import.");
            return list;
        }

        try {

            List<String> rows = IOUtils.readLines(new FileInputStream(this.locationListPath), Charsets.UTF_8);
            // exclude first row which contains the description
            rows = rows.subList(1, rows.size());
            for (String row : rows) {
                String[] splitRow = StringUtils.splitPreserveAllTokens(row, CSV_SEPARATOR);
                Optional<LocationRow> locationRow = getLocationRow(splitRow);
                locationRow.ifPresent(list::add);
            }
            logger.debug("Successfully imported LocationList.");

            return list;
        } catch (IOException e) {
            logger.error("Could not read file for LocationList.", e);
            // if we can't import the list, just return an empty list
            return list;
        }
    }


    private Optional<LocationRow> getLocationRow(String[] splitRow) {
        String id = splitRow[4];
        String type = splitRow[5];

        if (StringUtils.isNoneBlank(id, type)) {
            LocationRow.LocationRowBuilder builder = LocationRow.builder(id, type);
            String negOffset = splitRow[13];
            String posOffset = splitRow[14];
            String lat = splitRow[16];
            String lon = splitRow[17];
            return Optional.of(builder.addOffsets(negOffset, posOffset).addLatLon(lat, lon).build());
        }

        return Optional.empty();
    }
}
