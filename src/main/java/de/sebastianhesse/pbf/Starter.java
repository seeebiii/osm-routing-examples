package de.sebastianhesse.pbf;

import de.sebastianhesse.pbf.reader.OptimizedNodeEdgeReader;
import de.sebastianhesse.pbf.reader.PbfReader;


/**
 * Starter class to call different OSM reader.
 */
public class Starter {

    public static void pbfReader(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.exit(1);
        }

        String osmInputFile = args[0];
        String outputFileBase = args[1];
        PbfReader program = new PbfReader(osmInputFile, outputFileBase);
        program.importData();
        program.storeWaysAndNodesToFiles();
    }

    public static void nodeEdgeReader(String[] args) throws Exception {
        if (args == null || args.length != 1) {
            System.exit(1);
        }

        String osmInputFile = args[0];
        OptimizedNodeEdgeReader reader = new OptimizedNodeEdgeReader(osmInputFile);
        reader.importData();
    }


    public static void main(String[] args) throws Exception {
        // Uncomment the line for your desired osm reader
//        pbfReader(args);
        nodeEdgeReader(args);
    }
}
