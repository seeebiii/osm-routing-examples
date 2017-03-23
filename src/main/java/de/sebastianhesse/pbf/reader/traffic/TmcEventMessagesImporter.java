package de.sebastianhesse.pbf.reader.traffic;

import de.sebastianhesse.pbf.storage.traffic.TmcEvent;
import de.sebastianhesse.pbf.storage.traffic.TmcGroupEvent;
import de.sebastianhesse.pbf.storage.traffic.TmcSingleEvent;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Importer class for TMC traffic data for a certain hour. Can read a directory containing traffic data and read a
 * certain file from that directory. Also optimizes files ending with {@link #DAT_FILE_ENDING} by filtering out
 * useless entries and storing them in a new file ending with {@link #CDAT_FILE_ENDING}.
 */
public class TmcEventMessagesImporter {

    private static final Logger logger = LoggerFactory.getLogger(TmcEventMessagesImporter.class);
    private static final String CDAT_FILE_ENDING = "cdat";
    private static final String DAT_FILE_ENDING = "dat";
    private static final String ALLOWED_CDAT_FILE_NAME = "*HH.dat.cdat";
    private static final String ALLOWED_DAT_FILE_NAME = "*HH.dat";
    private static final String TYPE_SINGLE = "S";
    private static final String TYPE_GROUP = "GF";
    private static final char ROW_SEPARATOR_CHAR = ' ';

    private File trafficDataDirectory;


    public TmcEventMessagesImporter(String trafficDataDirectory) {
        if (StringUtils.isNotBlank(trafficDataDirectory)) {
            this.trafficDataDirectory = new File(trafficDataDirectory);
        }
    }


    /**
     * Filters all files within the given {@link #trafficDataDirectory} (if it exists) and returns a mapping for hour
     * and file. It's a must that each file has the format {@link #ALLOWED_DAT_FILE_NAME} or
     * {@link #ALLOWED_CDAT_FILE_NAME} to combine the hour with a file. Files ending with {@link #CDAT_FILE_ENDING}
     * are preferred over {@link #DAT_FILE_ENDING}, because they contain less entries and hence need less
     * time to import.
     *
     * @return a mapping for each hour of the day to a related file from the directory.
     */
    public TShortObjectMap<String> readDataDirectory() {
        TShortObjectMap<String> hourFileMapping = new TShortObjectHashMap<>(24);
        if (this.trafficDataDirectory == null || !this.trafficDataDirectory.exists()) {
            logger.warn("Folder is not set or does not exist, hence skipping import.");
            return hourFileMapping;
        }

        try {
            long start = System.currentTimeMillis();

            Iterator<File> fileIterator = FileUtils.iterateFiles(this.trafficDataDirectory, new String[]{CDAT_FILE_ENDING}, false);
            logger.debug("Start saving available *.cdat files in folder {}.", this.trafficDataDirectory);
            while (fileIterator.hasNext()) {
                File currentFile = fileIterator.next();
                handleAndSaveFile(hourFileMapping, currentFile);
            }

            logger.debug("Start saving available *.dat files in folder {}.", this.trafficDataDirectory);
            fileIterator = FileUtils.iterateFiles(this.trafficDataDirectory, new String[]{DAT_FILE_ENDING}, false);
            while (fileIterator.hasNext()) {
                File currentFile = fileIterator.next();
                handleAndSaveFile(hourFileMapping, currentFile);
            }

            logger.debug("Saved {} file names from folder. Took {} ms to save.",
                    hourFileMapping.size(), (start - System.currentTimeMillis()));
        } catch (IOException e) {
            logger.error("Error occurred while reading TMC message file or writing back optimized *.cdat file.", e);
        }

        return hourFileMapping;
    }


    private void handleAndSaveFile(TShortObjectMap<String> hourFileMapping, File currentFile) throws IOException {
        short hour = getHourFromFilename(currentFile);
        if (hour > -1 && hour < 24) {
            if (hourFileMapping.containsKey(hour)) {
                logger.warn("Skipped file, because hour {} has already been imported before.", hour);
            } else {
                hourFileMapping.put(hour, currentFile.getAbsolutePath());
            }
        } else {
            logger.warn("Skipped file {}, because the hour could not be retrieved from the file name. " +
                            "The file name does not match {} or {}.",
                    currentFile.getName(), ALLOWED_DAT_FILE_NAME, ALLOWED_CDAT_FILE_NAME);
        }
    }


    private short getHourFromFilename(File currentFile) {
        int idx = StringUtils.indexOf(currentFile.getName(), "." + CDAT_FILE_ENDING);
        if (idx < 0) {
            idx = StringUtils.indexOf(currentFile.getName(), "." + DAT_FILE_ENDING);
        } else {
            idx -= ("." + DAT_FILE_ENDING).length();
        }

        try {
            return Short.valueOf(StringUtils.substring(currentFile.getName(), idx - 2, idx));
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * Writes the (optimized) event list to a file if it's possible to write the file and the given file is not
     * already a {@link #CDAT_FILE_ENDING} file.
     *
     * @param file      a file ending with {@link #DAT_FILE_ENDING}
     * @param eventList an (optimized) list of TMC events
     * @return the path to the new {@link #CDAT_FILE_ENDING} file or to the existing {@link #DAT_FILE_ENDING} file if
     * it was not possible to write the new file
     * @throws IOException if some error occurred while writing the file
     */
    public String writeToFile(File file, List<TmcEvent> eventList) throws IOException {
        if (!isCdat(file)) {
            List<String> lines = eventList.stream().map(TmcEvent::getRow).collect(Collectors.toList());
            String cdatFilePath = getCdatFilePath(file);
            File cdatFile = new File(cdatFilePath);
            if (cdatFile.canWrite()) {
                FileUtils.writeLines(new File(cdatFilePath), Charsets.UTF_8.name(), lines);
                logger.info("Wrote optimized data to file {} to save time and space for the next import.", cdatFilePath);
                return cdatFilePath;
            } else {
                logger.warn("Skipped writing to file {}, because it was not possible to write the file.", cdatFilePath);
                return file.getAbsolutePath();
            }
        } else {
            return file.getAbsolutePath();
        }
    }


    /**
     * Reads a certain file which contains TMC traffic data. Only reads lines which are a single or group event.
     * Any line which produced an error while trying to read it will be ignored.
     *
     * @param file file containing TMC traffic data
     * @return list of events contained in the file
     * @throws IOException if some error occurred while reading the file
     */
    public List<TmcEvent> readFileData(File file) throws IOException {
        long start = System.currentTimeMillis();
        List<TmcEvent> eventList = new ArrayList<>();
        LineIterator lineIterator = FileUtils.lineIterator(file, Charsets.UTF_8.name());
        while (lineIterator.hasNext()) {
            String line = lineIterator.nextLine();
            if (StringUtils.isNotBlank(line)) {
                line = line.trim();
                String[] splitRow = StringUtils.split(line, ROW_SEPARATOR_CHAR);
                String type = splitRow[0];
                if (StringUtils.equalsIgnoreCase(TYPE_SINGLE, type) || StringUtils.equalsIgnoreCase(TYPE_GROUP, type)) {
                    try {
                        readSingleOrGroupEvent(eventList, line, splitRow, type);
                    } catch (Exception e) {
                        logger.warn("Skipping line {} in file {}, because an error occurred. Use log level debug " +
                                "to investigate the issue.", line, file.getName());
                        logger.debug("", e);
                    }
                }
            }
        }

        logger.debug("Took {} ms to load the data from file {}.", (start - System.currentTimeMillis()), file.getName());

        return eventList;
    }


    /**
     * Handles a TMC message event. Examples:
     * S evt=2028 loc=12283 ext=1 dur=0 dir=0 div=0
     * GF evt=802 loc=36553 ext=2 CI=3 dir=0
     * <p>
     * Converts them into a {@link TmcSingleEvent} or {@link TmcGroupEvent} and adds them to {@code eventList}.
     *
     * @param eventList list containing events which have been read before
     * @param line      the current line
     * @param splitRow  the line split in its different parts
     * @param type      either 'S' for {@link TmcSingleEvent} or 'GF' for {@link TmcGroupEvent}
     */
    private void readSingleOrGroupEvent(List<TmcEvent> eventList, String line, String[] splitRow, String type) {
        // examples:
        // S evt=2028 loc=12283 ext=1 dur=0 dir=0 div=0
        // GF evt=802 loc=36553 ext=2 CI=3 dir=0
        String eventCode = getParamValue(splitRow[1]);
        String locationCode = getParamValue(splitRow[2]);
        String extend = getParamValue(splitRow[3]);
        String direction = getParamValue(splitRow[5]);
        switch (type) {
            case TYPE_SINGLE:
                eventList.add(new TmcSingleEvent(line, i(eventCode), i(locationCode), s(extend), b(direction)));
                break;
            case TYPE_GROUP:
                eventList.add(new TmcGroupEvent(line, i(eventCode), i(locationCode), s(extend), b(direction)));
                break;
        }
    }


    private String getParamValue(String param) {
        return StringUtils.split(param, "=")[1];
    }


    private int i(String value) {
        return Integer.valueOf(value);
    }


    private short s(String value) {
        return Short.valueOf(value);
    }


    /**
     * '1' => true
     * '0' or something else => false
     *
     * @param value '1' or something else
     * @return true if '1', false otherwise
     */
    private boolean b(String value) {
        return value != null && value.charAt(0) == '1';
    }


    private String getCdatFilePath(File file) {
        if (isCdat(file)) {
            return file.getAbsolutePath();
        } else {
            return file.getAbsolutePath() + "." + CDAT_FILE_ENDING;
        }
    }


    private boolean isCdat(File file) {
        return StringUtils.endsWith(file.getAbsolutePath(), "." + CDAT_FILE_ENDING);
    }
}
