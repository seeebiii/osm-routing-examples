package de.sebastianhesse.pbf.dropwizard.resources;

import de.sebastianhesse.pbf.dropwizard.resources.dto.LatLngList;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;


/**
 * Resource to retrieves POIs like gas stations.
 */
@Path("/gasstations")
@Produces(MediaType.APPLICATION_JSON)
public class GasStationResource {

    private Graph graph;


    public GasStationResource(Graph graph) {
        this.graph = graph;
    }


    @GET
    public Response getGasStations(@QueryParam("lat") double lat, @QueryParam("lon") double lon,
                                   @QueryParam("maxDistance") @DefaultValue("10") short maxDistance) {
        Optional<Node> source = this.graph.findClosestNode(lat, lon);
        if (source.isPresent()) {
            return Response.ok(new LatLngList(graph.getGasStationsAround(source.get(), maxDistance))).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
