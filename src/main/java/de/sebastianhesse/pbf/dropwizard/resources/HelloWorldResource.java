package de.sebastianhesse.pbf.dropwizard.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;


/**
 * 'Hello World' resource to test that server was setup correctly.
 */
@Path("/hello")
public class HelloWorldResource {

    @GET
    public String hello() {
        return "Hello!";
    }
}
