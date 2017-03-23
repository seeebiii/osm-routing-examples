package de.sebastianhesse.pbf.dropwizard.resources;

import de.sebastianhesse.pbf.dropwizard.resources.dto.TrafficWaysDto;
import de.sebastianhesse.pbf.storage.Node;
import de.sebastianhesse.pbf.storage.traffic.TrafficHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


/**
 * Resource to get the traffic status, add or remove traffic data to the graph.
 */
@Path("/traffic")
@Produces(MediaType.APPLICATION_JSON)
public class TrafficResource {

    private static final Logger logger = LoggerFactory.getLogger(TrafficResource.class);

    private TrafficHandler trafficHandler;


    public TrafficResource(TrafficHandler trafficHandler) {
        this.trafficHandler = trafficHandler;
    }


    @GET
    public Response getCurrentTrafficHour() {
        return Response.ok(new TrafficWaysDto(trafficHandler.getLastHour(), trafficHandler.getLastUpdatedWays())).build();
    }


    @PUT
    @Path("/{hour}")
    public Response readTrafficForHour(@PathParam("hour") short hour) throws Exception {
        long start = System.currentTimeMillis();
        List<Pair<Node, Node>> updatedWays = this.trafficHandler.updateTraffic(hour);
        TrafficWaysDto entity = new TrafficWaysDto(hour, updatedWays);
        logger.info("Complete time to update traffic data and build response: {} ms", (System.currentTimeMillis() - start));
        return Response.ok(entity).build();
    }


    @DELETE
    public Response removeTrafficData() {
        long start = System.currentTimeMillis();
        this.trafficHandler.removeTrafficData();
        logger.info("Complete time to delete traffic data: {} ms", (System.currentTimeMillis() - start));
        return Response.ok().build();
    }
}
