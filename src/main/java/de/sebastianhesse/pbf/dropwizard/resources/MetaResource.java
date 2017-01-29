package de.sebastianhesse.pbf.dropwizard.resources;

import de.sebastianhesse.pbf.dropwizard.DropwizardConfiguration;
import de.sebastianhesse.pbf.dropwizard.resources.dto.MetaDto;
import de.sebastianhesse.pbf.storage.Graph;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;


/**
 * Resource to retrieve some meta information about the OSM Routing backend.
 */
@Path("/meta")
@Produces(MediaType.APPLICATION_JSON)
public class MetaResource {

    private DropwizardConfiguration configuration;
    private String osmFile;
    private Graph graph;


    public MetaResource(DropwizardConfiguration configuration, String osmFile, Graph graph) {
        this.configuration = configuration;
        this.osmFile = getFileName(osmFile);
        this.graph = graph;
    }


    @GET
    public Response getMetaData() {
        String readerStrategy = this.configuration.getReaderStrategy().toString();
        return Response.ok(new MetaDto(osmFile, readerStrategy, graph.getEdgesSize(), graph.getNodesSize())).build();
    }


    private String getFileName(String osmFile) {
        int i = StringUtils.lastIndexOf(osmFile, File.separatorChar);
        return osmFile.substring(i + 1);
    }
}
