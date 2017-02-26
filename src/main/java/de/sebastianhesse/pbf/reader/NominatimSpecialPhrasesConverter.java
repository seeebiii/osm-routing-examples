package de.sebastianhesse.pbf.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Converts a file containing Nominatim special phrases.
 * Result is stored as JSON, so a frontend script can easily read the values.
 * Data extracted from http://wiki.openstreetmap.org/w/index.php?title=Nominatim/Special_Phrases/DE
 */
public class NominatimSpecialPhrasesConverter {

    private static final Logger logger = LoggerFactory.getLogger(NominatimSpecialPhrasesConverter.class);

    private String inputFile = "";
    private InputStream inputStream = null;
    private Map<String, Set<String>> phrases = new HashMap<>();;


    public NominatimSpecialPhrasesConverter(String inputFile) {
        if (StringUtils.isBlank(inputFile)) {
            throw new IllegalArgumentException("You must provide a filename.");
        }
        this.inputFile = inputFile;
    }


    public NominatimSpecialPhrasesConverter(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("You must provide an InputStream.");
        }
        this.inputStream = stream;
    }


    public Map<String, Set<String>> convert() {
        if (StringUtils.isBlank(this.inputFile)) {
            throw new IllegalStateException("Only call this method if you're reading a file from the file system.");
        }
        try (FileReader reader = new FileReader(inputFile)) {
            readFile(reader);
        } catch (IOException e) {
            logger.error("Error reading Nominatim file.", e);
            throw new RuntimeException("Could not read Nominatim file.");
        }

        return this.phrases;
    }

    public Map<String, Set<String>> convertFromStream() {
        if (this.inputStream == null) {
            throw new IllegalStateException("Only call this method if you're using an InputStream.");
        }
        readFile(new InputStreamReader(inputStream));
        return this.phrases;
    }


    private void readFile(Reader reader) {
        LineIterator lineIterator = IOUtils.lineIterator(reader);

        while (lineIterator.hasNext()) {
            String line = lineIterator.nextLine();
            // ignore comments and other lines, just parse lines with phrases
            if (StringUtils.startsWith(line, "| ")) {
                handleKeyValuePair(line);
            }
        }
    }


    private void handleKeyValuePair(String line) {
        // extract key and value pair
        String[] parts = StringUtils.split(line, "||");
        String key = parts[1].trim();
        String value = parts[2].trim();

        // add value to list of values for key
        Set<String> values = null;
        if (this.phrases.containsKey(key)) {
            values = this.phrases.get(key);
        } else {
            // first value to add, thus creating a new Set
            values = new HashSet<>();
            this.phrases.put(key, values);
        }
        values.add(value);
    }


    public Map<String, Set<String>> convertFromJsonStream() {
        if (this.inputStream == null) {
            throw new IllegalStateException("Only call this method if you're using an InputStream.");
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(this.inputStream);
            Iterator<Map.Entry<String, JsonNode>> iter = jsonNode.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> next = iter.next();
                String key = next.getKey();
                JsonNode value = next.getValue();
                Set<String> values = new HashSet<>(value.size());
                value.forEach(element -> {
                    values.add(element.textValue());
                });
                this.phrases.put(key, values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.phrases;
    }


    public String saveToOutputFile(String filepath) {
        String outputFile = filepath;
        if (StringUtils.isBlank(outputFile)) {
            outputFile = StringUtils.removeEndIgnoreCase(this.inputFile, ".txt") + ".json";
        }
        File file = new File(outputFile);
        resetFileIfExists(file);

        try (FileWriter writer = new FileWriter(file)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, this.phrases);
        } catch (IOException e) {
            logger.error("Error writing Nominatim phrases to output file. ", e);
            throw new RuntimeException("Could not write Nominatim phrases as JSON to output file.");
        }
        return outputFile;
    }


    private void resetFileIfExists(File outputFile) {
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


    public static void main(String[] args) {
        NominatimSpecialPhrasesConverter converter = new NominatimSpecialPhrasesConverter(args[0]);
        converter.convert();
        converter.saveToOutputFile(null);
    }
}
