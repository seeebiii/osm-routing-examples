package de.sebastianhesse.pbf.dropwizard.resources;

import de.sebastianhesse.pbf.dropwizard.resources.dto.LatLngList;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;


/**
 * Resource to retrieves POIs like gas stations.
 */
@Path("/pois")
@Produces(MediaType.APPLICATION_JSON)
public class PoiResource {

    private Graph graph;


    public PoiResource(Graph graph) {
        this.graph = graph;
    }


    @GET
    public Response getPois(@QueryParam("lat") double lat, @QueryParam("lon") double lon,
                            @QueryParam("maxDistance") @DefaultValue("10") short maxDistance,
                            @QueryParam("typeKey") String typeKey, @QueryParam("typeValue") String typeValue) {
        Optional<Node> source = this.graph.findClosestNode(lat, lon);
        if (source.isPresent()) {
            List<Node> pois = graph.getPoisAround(source.get(), maxDistance, new ImmutablePair<>(typeKey, typeValue));
            return Response.ok(new LatLngList(pois)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }


    @OPTIONS
    public Response retrieveOptions() {
        return Response.ok(this.graph.getPoiTypes()).build();
    }
}
