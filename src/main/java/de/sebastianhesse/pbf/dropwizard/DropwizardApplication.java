package de.sebastianhesse.pbf.dropwizard;

import de.sebastianhesse.pbf.dropwizard.healtchecks.GraphHealthCheck;
import de.sebastianhesse.pbf.dropwizard.healtchecks.StrategyHealthCheck;
import de.sebastianhesse.pbf.dropwizard.resources.HelloWorldResource;
import de.sebastianhesse.pbf.dropwizard.resources.PoiResource;
import de.sebastianhesse.pbf.dropwizard.resources.MetaResource;
import de.sebastianhesse.pbf.dropwizard.resources.RoutingResource;
import de.sebastianhesse.pbf.reader.NodeEdgeReader;
import de.sebastianhesse.pbf.reader.OptimizedNodeEdgeReader;
import de.sebastianhesse.pbf.reader.SimpleNodeEdgeReader;
import de.sebastianhesse.pbf.storage.Graph;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


/**
 * Application to start up a dropwizard server. Configures resources, health checks and loads the OSM file.
 */
public class DropwizardApplication extends Application<DropwizardConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DropwizardApplication.class);

    private String osmFile = "";


    public DropwizardApplication(String osmFile) {
        this.osmFile = osmFile;
        logger.info("Using file: ", osmFile);
    }


    public static void main(String[] args) throws Exception {
        // strip out the osm file path, otherwise Dropwizard complains about an unknown parameter;
        // the unknown parameter could be addressed by adding a custom command, but it's okay for this use case
        String[] argsWithoutOsmFile = Arrays.copyOfRange(args, 0, 2);
        new DropwizardApplication(args[2]).run(argsWithoutOsmFile);
    }


    @Override
    public String getName() {
        return "dropwizard-osm-server";
    }


    @Override
    public void initialize(Bootstrap<DropwizardConfiguration> bootstrap) {
        // in order to serve HTML files and alike, the root path of the server serves our assets
        bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
        bootstrap.addBundle(new AssetsBundle("/assets/css", "/css", null, "css"));
        bootstrap.addBundle(new AssetsBundle("/assets/js", "/js", null, "js"));
        bootstrap.addBundle(new AssetsBundle("/assets/fonts", "/fonts", null, "fonts"));
    }


    @Override
    public void run(DropwizardConfiguration configuration,
                    Environment environment) {
        // set url pattern for resources, i.e. they can be accessed by /api/{resourcePath}
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new HelloWorldResource());

        // import OSM data and register routing resource
        NodeEdgeReader reader = getNodeEdgeReader(configuration);
        Graph graph = null;

        try {
            reader.importData();
            graph = reader.getGraph();
        } catch (Exception e) {
            logger.info("Something went wrong while reading OSM data. See error log.");
            logger.error("", e);
        }

        final RoutingResource routingResource = new RoutingResource(graph);
        environment.jersey().register(routingResource);

        final PoiResource poiResource = new PoiResource(graph);
        environment.jersey().register(poiResource);

        final MetaResource metaResource = new MetaResource(configuration, osmFile, graph);
        environment.jersey().register(metaResource);

        // health checks
        environment.healthChecks().register("GraphHealthCheck", new GraphHealthCheck(graph));
        environment.healthChecks().register("ReaderStrategyHealthCheck", new StrategyHealthCheck(configuration));
    }


    private NodeEdgeReader getNodeEdgeReader(DropwizardConfiguration configuration) {
        NodeEdgeReader reader;

        switch (configuration.getReaderStrategy()) {
            case OPTIMIZED:
                reader = new OptimizedNodeEdgeReader(osmFile);
                break;
            case SIMPLE:
            default:
                reader = new SimpleNodeEdgeReader(osmFile);
                break;
        }

        return reader;
    }

}
