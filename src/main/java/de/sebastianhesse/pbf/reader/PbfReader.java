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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(PbfReader.class);

    protected TLongObjectMap<ReaderElement> nodes = new TLongObjectHashMap<>();
    protected TLongIntMap nodeCounter = new TLongIntHashMap();
    protected List<Object> ways = new ArrayList<>();
    protected DistanceCalc distanceCalc = new DistanceCalcEarth();
    protected ReaderElementValidator validator = new ReaderElementValidator();
    protected File osmFile = null;

    private String outputFilePath = null;


    public PbfReader(String filePath, String outputFilePath) {
        this.osmFile = new File(filePath);
        this.outputFilePath = outputFilePath;
    }


    public PbfReader importData() throws Exception {
        if (this.osmFile != null && this.osmFile.exists()) {
            logger.info("Starting to import data.");

            long start = System.currentTimeMillis();
            logger.info("--------------------------- READ WAYS ------------------------");
            readWays();
            logger.info("--------------------------- READ NODES -----------------------");
            readNodesOfWays();
            this.nodeCounter.clear();

            logger.info("Loaded " + this.ways.size() + " ways into memory.");
            logger.info("Loaded " + this.nodes.size() + " nodes into memory.");
            logger.info("It took " + (System.currentTimeMillis() - start) / 1000 + " seconds to import the data.");
        } else {
            logger.info("Could not start the task, because the OSM file does not exist.");
        }

        return this;
    }


    protected void readWays() throws Exception {
        processFile("way", item -> {
            switch (item.getType()) {
                case ReaderElement.WAY:
                    ReaderWay way = (ReaderWay) item;
                    final TLongList nodeList = way.getNodes();

                    if (this.validator.isValidWay(way)) {
                        processWayNodes(nodeList);
                        this.ways.add(way);
                        return true;
                    }
            }

            return false;
        });
    }


    protected void processWayNodes(TLongList nodeList) {
        // according to documentation there are max. 2000 nodes in a list
        short size = (short) nodeList.size();
        for (int i = 0; i < size; i++) {
            final long id = nodeList.get(i);
            if (this.nodeCounter.containsKey(id)) {
                this.nodeCounter.put(id, this.nodeCounter.get(id) + 1);
            } else {
                this.nodeCounter.put(id, 1);
            }
        }
    }


    protected void readNodesOfWays() throws Exception {
        processFile("node", item -> {
            switch (item.getType()) {
                case ReaderElement.NODE:
                    ReaderNode node = (ReaderNode) item;

                    if (this.nodeCounter.containsKey(node.getId())) {
                        this.nodes.put(node.getId(), node);
                        this.nodeCounter.remove(node.getId());

                        return true;
                    }
            }

            return false;
        });
    }


    protected void processFile(String type, ItemHandler itemHandler) throws Exception {
        final OSMInputFile osmInputFile = new OSMInputFile(this.osmFile).setWorkerThreads(2).open();
        int counter = 0;
        ReaderElement item = null;
        while ((item = osmInputFile.getNext()) != null) {
            boolean handled = itemHandler.handle(item);

            if (handled && ++counter % 100000 == 0) {
                logger.info("Imported " + counter + " objects of type " + type);
            }
        }
    }


    public void storeWaysAndNodesToFiles() throws Exception {
        int fileCounter = 0;
        int counter = 0;
        StringBuilder builder = new StringBuilder();
        for (ReaderElement nodeItem : this.nodes.valueCollection()) {
            builder.append(getNodeAsString(nodeItem));
            counter++;

            if (counter % 1000000 == 0) {
                write(builder, fileCounter++);
                builder = new StringBuilder();
                logger.info("Wrote " + counter + " nodes to file.");
            }
        }
        write(builder, fileCounter++);
        logger.info("Wrote " + counter + " nodes to file.");

        counter = 0;
        for (Object item : this.ways) {
            builder.append(getWayAsString(item));
            counter++;

            if (counter % 100000 == 0) {
                write(builder, fileCounter++);
                builder = new StringBuilder();
                logger.info("Wrote " + counter + " ways to file.");
            }
        }
        write(builder, fileCounter);
        logger.info("Wrote " + counter + " ways to file.");
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
}