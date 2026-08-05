package org.fnm;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/simulation/v1/report")
public class SimulationReport {

    private static final Logger LOG = Logger.getLogger(SimulationReport.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "Please use the post method to send simulation reports.";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(@QueryParam("session") String simulationId, String body) {

        if (simulationId == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        if (body == null || body.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).build();

        LOG.info("Received simulation report: " + simulationId);
        LOG.info("Report: " + body);

        return Response.ok().build();
    }

}
