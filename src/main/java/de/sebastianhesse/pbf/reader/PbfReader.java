package de.sebastianhesse.pbf.reader;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This example class reads an OSM-PBF file and outputs all ways and their related nodes to multiple output files.
 * Run the {@link #main(String[])} method with the parameters pointing to an OSM file and hand over a base path to a
 * file which will be used to store the results.
 * <p>
 * For example:
 * "/tmp/my.osm.pbf" "/tmp/my_result.txt" => will create files like "/tmp/my_result.txt0", "/tmp/my_result.txt1", ...
 */
public class PbfReader {

    private TLongObjectMap<ReaderElement> nodes = new TLongObjectHashMap<>();
    private TLongIntMap nodeCounter = new TLongIntHashMap();
    private List<Object> ways = new ArrayList<>();
    private DistanceCalc distanceCalc = new DistanceCalcEarth();
    private File osmFile = null;
    private String outputFilePath = null;

    public PbfReader(String filePath, String outputFilePath) {
        this.osmFile = new File(filePath);
        this.outputFilePath = outputFilePath;
    }


    public PbfReader importData() throws Exception {
        if (this.osmFile != null && this.osmFile.exists()) {
            System.out.println("Starting to import data.");

            long start = System.currentTimeMillis();
            readWays();
            readNodesOfWays();
            this.nodeCounter.clear();
            long end = System.currentTimeMillis();
            long importTime = end - start;

            start = System.currentTimeMillis();
            storeWaysAndNodesToFiles();
            end = System.currentTimeMillis();

            System.out.println("Loaded " + this.ways.size() + " ways into memory.");
            System.out.println("Loaded " + this.nodes.size() + " nodes into memory.");

            System.out.println("It took " + (importTime) / 1000 + " seconds to import the data.");
            System.out.println("It took " + (end - start) / 1000 + " seconds to write the data.");
        } else {
            System.out.println("Could not start the task, because the OSM file does not exist.");
        }

        return this;
    }

    private void readWays() throws Exception {
        processFile("way", item -> {
            switch (item.getType()) {
                case ReaderElement.WAY:
                    ReaderWay way = (ReaderWay) item;
                    final TLongList nodeList = way.getNodes();
                    final int size = nodeList.size();

                    if (size > 1 && !(way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))) {
                        processWayNodes(nodeList, size);
                        this.ways.add(ArrayUtils.addAll(nodeList.toArray(), way.getId()));
                        return true;
                    }
            }

            return false;
        });
    }

    private void processWayNodes(TLongList nodeList, int size) {
        for (int i = 0; i < size; i++) {
            final long id = nodeList.get(i);
            if (this.nodeCounter.containsKey(id)) {
                this.nodeCounter.put(id, this.nodeCounter.get(id) + 1);
            } else {
                this.nodeCounter.put(id, 1);
            }
        }
    }

    private void readNodesOfWays() throws Exception {
        processFile("node", item -> {
            switch (item.getType()) {
                case ReaderElement.NODE:
                    ReaderNode node = (ReaderNode) item;

                    if (this.nodeCounter.containsKey(node.getId())) {
                        this.nodes.put(node.getId(), node);
                        int nodeCounter = this.nodeCounter.get(node.getId());
                        if (--nodeCounter == 0) {
                            this.nodeCounter.remove(node.getId());
                        }

                        return true;
                    }
            }

            return false;
        });
    }

    private void storeWaysAndNodesToFiles() throws Exception {
        int fileCounter = 0;
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (ReaderElement nodeItem : this.nodes.valueCollection()) {
            builder.append(getNodeAsString(nodeItem));
            counter++;

            if (counter % 1000000 == 0) {
                write(builder, fileCounter++);
                builder = new StringBuilder();
                System.out.println("Wrote " + counter + " nodes to file.");
            }
        }
        write(builder, fileCounter++);
        System.out.println("Wrote " + counter + " nodes to file.");

        counter = 0;
        for (Object item : this.ways) {
            builder.append(getWayAsString(item));
            counter++;

            if (counter % 100000 == 0) {
                write(builder, fileCounter++);
                builder = new StringBuilder();
                System.out.println("Wrote " + counter + " ways to file.");
            }
        }
        write(builder, fileCounter);
        System.out.println("Wrote " + counter + " ways to file.");
    }

    private StringBuilder getNodeAsString(ReaderElement nodeItem) throws Exception {
        ReaderNode node = (ReaderNode) nodeItem;
        StringBuilder builder = new StringBuilder();
        builder.append("Node[").append(node.getId()).append("]: (")
                .append(node.getLat()).append(",").append(node.getLon())
                .append(")\r\n");
        return builder;
    }


    private StringBuilder getWayAsString(Object item) throws Exception {
        long[] way = (long[]) item;
        StringBuilder builder = new StringBuilder();
        builder.append("Way[").append(way[way.length - 1]).append("]: (");
        long distance = 0;

        for (int i = 0; i < way.length - 1; i++) {
            long nodeId = way[i];
            ReaderElement nodeItem = this.nodes.get(nodeId);

            if (nodeItem != null) {
                ReaderNode node = (ReaderNode) nodeItem;
                builder.append("[").append(node.getLat()).append(",").append(node.getLon()).append("]");

                if (i < way.length - 2) {
                    // calc distance to next point if it's not the last point
                    long nextNodeId = way[i + 1];
                    ReaderElement nextNodeItem = this.nodes.get(nextNodeId);
                    if (nextNodeItem != null) {
                        ReaderNode nextNode = (ReaderNode) nextNodeItem;
                        distance += distanceCalc.calcDist(node.getLat(), node.getLon(), nextNode.getLat(), nextNode.getLon());
                    }
                    builder.append(", ");
                }
            }
        }
        builder.append(")").append(" Distance: ").append(distance).append("\r\n");
        return builder;
    }

    private void write(StringBuilder buffer, int fileNumber) throws IOException {
        File outputFile = new File(this.outputFilePath + fileNumber);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        boolean fileAvailable = !outputFile.exists() && outputFile.createNewFile();
        if (fileAvailable) {
            OutputStream outputStream = new FileOutputStream(outputFile);
            try {
                IOUtils.write(buffer.toString(), outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    private void processFile(String type, ItemHandler itemHandler) throws Exception {
        final OSMInputFile osmInputFile = new OSMInputFile(this.osmFile).setWorkerThreads(2).open();
        int counter = 0;
        ReaderElement item = null;
        while ((item = osmInputFile.getNext()) != null) {
            boolean handled = itemHandler.handle(item);

            if (handled && ++counter % 100000 == 0) {
                System.out.println("Imported " + counter + " objects of type " + type);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.exit(1);
        }

        String osmInputFile = args[0];
        String outputFileBase = args[1];
        PbfReader program = new PbfReader(osmInputFile, outputFileBase);
        program.importData();
    }
}

@FunctionalInterface
interface ItemHandler {

    boolean handle(ReaderElement item);
}
