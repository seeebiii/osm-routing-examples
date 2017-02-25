package de.sebastianhesse.pbf.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Converts a file containing Nominatim special phrases.
 * Result is stored as JSON, so a frontend script can easily read the values.
 * Data extracted from http://wiki.openstreetmap.org/w/index.php?title=Nominatim/Special_Phrases/DE
 */
public class NominatimSpecialPhrasesConverter {

    public static void main(String[] args) {
        // prepare input and output file
        String filepath = args[0];
        if (StringUtils.isBlank(filepath)) {
            System.out.println("You must provide a filename.");
            System.exit(1);
        }
        String outputFilepath = StringUtils.removeEndIgnoreCase(filepath, ".txt") + ".json";
        File originalFile = new File(filepath);
        File outputFile = new File(outputFilepath);
        resetFileIfExists(outputFile);

        // read each line and put key value pairs into a map. each key can have multiple values, thus Set<String>
        LineIterator lineIterator = null;
        Map<String, Set<String>> phrases = new HashMap<>();

        try (FileReader reader = new FileReader(originalFile); FileWriter writer = new FileWriter(outputFile)){
            lineIterator = IOUtils.lineIterator(reader);

            while (lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                // ignore comments and other lines, just parse lines with phrases
                if (StringUtils.startsWith(line, "| ")) {
                    handleKeyValuePair(phrases, line);
                }
            }

            // write phrases to output file as json
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, phrases);
        } catch (IOException e) {
            System.out.println("Something went wrong when reading/writing the input/output file.");
            e.printStackTrace();
        }
    }


    private static void resetFileIfExists(File outputFile) {
        if (outputFile.exists()) {
            outputFile.delete();
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }


    private static void handleKeyValuePair(Map<String, Set<String>> phrases, String line) {
        // extract key and value pair
        String[] parts = StringUtils.split(line, "||");
        String key = parts[1].trim();
        String value = parts[2].trim();

        // add value to list of values for key
        Set<String> values = null;
        if (phrases.containsKey(key)) {
            values = phrases.get(key);
        } else {
            // first value to add, thus creating a new Set
            values = new HashSet<>();
            phrases.put(key, values);
        }
        values.add(value);
    }
}
