package de.sebastianhesse.pbf.dropwizard.healtchecks;

import com.codahale.metrics.health.HealthCheck;
import de.sebastianhesse.pbf.storage.Graph;


/**
 * Checks that the graph has been loaded correctly.
 */
public class GraphHealthCheck extends HealthCheck {

    private Graph graph;


    public GraphHealthCheck(Graph graph) {
        this.graph = graph;
    }


    @Override
    protected Result check() throws Exception {
        if (this.graph != null && this.graph.getEdges().length > 0 && this.graph.getNodes().length > 0) {
            return Result.healthy();
        }
        return Result.unhealthy("Graph is either not initialized or has zero nodes/edges.");
    }
}
