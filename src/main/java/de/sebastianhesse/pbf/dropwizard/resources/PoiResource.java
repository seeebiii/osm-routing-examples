package de.sebastianhesse.pbf.dropwizard.resources;

import de.sebastianhesse.pbf.dropwizard.resources.dto.PoiSearchDto;
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
                            @QueryParam("pid") @DefaultValue("-1") String pid,
                            @QueryParam("maxDistance") @DefaultValue("10") short maxDistance,
                            @QueryParam("typeKey") String typeKey, @QueryParam("typeValue") String typeValue) {
        int pointId = getIdAsInt(pid);
        Optional<Node> source = this.graph.findClosestNode(pointId, lat, lon);
        if (source.isPresent()) {
            List<Node> pois = graph.getPoisAround(source.get(), maxDistance, new ImmutablePair<>(typeKey, typeValue));
            return Response.ok(new PoiSearchDto(source.get(), pois)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }


    @OPTIONS
    public Response retrieveOptions() {
        return Response.ok(this.graph.getPoiTypes()).build();
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
}
