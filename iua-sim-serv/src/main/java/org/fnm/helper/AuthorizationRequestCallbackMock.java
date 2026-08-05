package org.fnm.helper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@Path("/callback")
public class AuthorizationRequestCallbackMock {

    private static final Logger LOG = Logger.getLogger(AuthorizationRequestCallbackMock.class);

    @GET
    public Response get(@Context HttpHeaders headers,
                        @QueryParam("authorization_code") String authCode,
                        @QueryParam("state") String state) {

        String msg = "Callback mock called with authorization code = " + authCode + " and state = " + state;
        LOG.info(msg);
        return Response.ok(msg).build();
    }

}
