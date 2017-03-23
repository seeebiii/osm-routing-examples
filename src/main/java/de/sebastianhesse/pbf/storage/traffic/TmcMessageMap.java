package de.sebastianhesse.pbf.storage.traffic;

import de.sebastianhesse.pbf.reader.traffic.TmcEventMessagesImporter;
import gnu.trove.map.TShortObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Holds the important information which files can be imported for certain traffic hours.
 */
public class TmcMessageMap {

    private static final Logger logger = LoggerFactory.getLogger(TmcMessageMap.class);

    private TmcEventMessagesImporter importer;
    private TShortObjectMap<String> hourFileMapping;


    public TmcMessageMap(TmcEventMessagesImporter importer) {
        this.importer = importer;
        this.hourFileMapping = importer.readDataDirectory();
        logger.debug("Read contents of data directory with traffic data files.");
    }


    /**
     * Reads a file containing TMC traffic data of a certain hour.
     *
     * @param hour between 0 and 23 (both inclusive)
     * @return all TMC events of the file, without any guaranty of the order
     * @throws IllegalArgumentException if the requested hour is not between 0 and 23 (inclusive) or no file is available
     * @throws IOException              if the traffic file can't be read
     */
    public List<TmcEvent> readFile(short hour) throws IOException {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("An hour must be between 0 and 23.");
        } else if (!this.hourFileMapping.containsKey(hour)) {
            throw new IllegalArgumentException("No file to import for hour " + hour);
        }
        String filename = this.hourFileMapping.get(hour);
        try {
            List<TmcEvent> eventList = importer.readFileData(new File(filename));
            eventList = eventList.parallelStream().unordered().distinct().collect(Collectors.toList());
            // store optimized data in cdat file for next import
            String cdatFilename = importer.writeToFile(new File(filename), eventList);
            this.hourFileMapping.put(hour, cdatFilename);
            return eventList;
        } catch (IOException e) {
            logger.error("Error occurred while reading TMC message file {} for hour {}.", filename, hour);
            throw e;
        }
    }
}
