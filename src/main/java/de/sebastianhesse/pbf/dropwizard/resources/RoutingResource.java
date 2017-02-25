package de.sebastianhesse.pbf.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;
import de.sebastianhesse.pbf.dropwizard.resources.dto.LatLngList;
import de.sebastianhesse.pbf.reader.Accessor;
import de.sebastianhesse.pbf.routing.Dijkstra;
import de.sebastianhesse.pbf.routing.DijkstraOptions;
import de.sebastianhesse.pbf.routing.calculators.CalculationType;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;


/**
 * A resource to accept routing calls between two points.
 */
@Path("/route")
@Produces(MediaType.APPLICATION_JSON)
public class RoutingResource {

    private static final Logger logger = LoggerFactory.getLogger(RoutingResource.class);

    private Graph graph;


    public RoutingResource(Graph graph) {
        this.graph = graph;
    }


    /**
     * Calculates a path for a certain vehicle between two points (lat1,lon1) and (lat2,lon2).
     * Calculation depends on vehicle type and path mode, e.g. fastest or shortest.
     * @param lat1 latitude for point 1
     * @param lon1 longitude for point 1
     * @param lat2 latitude for point 2
     * @param lon2 latitude for point 2
     * @param vehicle vehicle type
     * @param mode calculation mode
     * @return 200 if a path could be found; response body contains list of points, see {@link LatLngList}
     *         409 if points can not be found in graph OR if there is now way between them
     *         500 if something unexpected happens while retrieving the path
     * @see DijkstraOptions for vehicle and mode
     */
    @GET
    @Timed
    public Response getRouteForPoints(@QueryParam("lat1") double lat1, @QueryParam("lon1") double lon1,
                                      @QueryParam("pid1") @DefaultValue("-1") String pid1,
                                      @QueryParam("lat2") double lat2, @QueryParam("lon2") double lon2,
                                      @QueryParam("pid2") @DefaultValue("-1") String pid2,
                                      @QueryParam("vehicle") String vehicle, @QueryParam("mode") String mode) {
        Accessor accessor = Accessor.valueOf(vehicle.toUpperCase());
        CalculationType calculationType = CalculationType.valueOf(mode.toUpperCase());
        DijkstraOptions dijkstraOptions = new DijkstraOptions(accessor, calculationType);

        int node1Id = getIdAsInt(pid1);
        int node2Id = getIdAsInt(pid2);

        Optional<Node> startNodeOptional = graph.findClosestNode(node1Id, lat1, lon1);
        Optional<Node> endNodeOptional = graph.findClosestNode(node2Id, lat2, lon2);

        if (startNodeOptional.isPresent() && endNodeOptional.isPresent()) {
            Node startNode = startNodeOptional.get();
            Node endNode = endNodeOptional.get();
            return getShortestPathWithDijkstra(dijkstraOptions, startNode, endNode);
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Can not locate start or end node with given values.")
                    .build();
        }
    }


    private int getIdAsInt(String pid) {
        try {
            return Integer.valueOf(pid);
        } catch (NumberFormatException e) {
            // if it's not an int, try as double
            try {
                return Double.valueOf(pid).intValue();
            } catch (NumberFormatException e1) {
                return -1;
            }
        }
    }


    private Response getShortestPathWithDijkstra(DijkstraOptions dijkstraOptions, Node startNode, Node endNode) {
        Dijkstra dijkstra = new Dijkstra(graph, startNode, endNode, dijkstraOptions);
        dijkstra.run();
        try {
            dijkstra.join();
            List<Node> nodes = dijkstra.retrieveShortestPath();
            if (nodes.size() == 0) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Could not find an existent way between given points.")
                        .build();
            } else {
                return Response.ok(new LatLngList(nodes)).build();
            }
        } catch (InterruptedException e) {
            logger.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
