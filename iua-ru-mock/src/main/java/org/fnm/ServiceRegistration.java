package org.fnm;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/simulation/v1/registration")
public class ServiceRegistration {

    private static final Logger LOG = Logger.getLogger(ServiceRegistration.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "Please use the post method to register a simulation service.";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(String body) {

        if (body == null || body.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).build();

        LOG.info("Received Service Registration: " + body);

        return Response.ok().build();
    }

}
