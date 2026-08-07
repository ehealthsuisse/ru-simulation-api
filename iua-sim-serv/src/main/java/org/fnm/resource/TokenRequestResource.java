package org.fnm.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.fnm.AuthorizationService;
import org.fnm.helper.GrantType;
import org.fnm.model.TokenRequestParameter;
import org.jboss.logging.Logger;

import java.util.Base64;
import java.util.List;

@Path("/token")
public class TokenRequestResource {

    private static final Logger LOG = Logger.getLogger(TokenRequestResource.class);

    @Inject
    AuthorizationService authorizationService;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(@Context HttpHeaders headers, MultivaluedMap<String, String> formParams) throws Exception {

        // get authorization header
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        String authHeader = requestHeaders.getFirst("Authorization");
        List<String> headerData = parseAuthorizationHeader(authHeader);

        // create container for request parameter
        TokenRequestParameter tokenRequestParameter = new TokenRequestParameter();

        // common for both types
        tokenRequestParameter.grantType = formParams.getFirst("grant_type");
        tokenRequestParameter.clientId = headerData.get(0);
        tokenRequestParameter.clientSecret = headerData.get(1);

        // CC flow only
        tokenRequestParameter.principal = formParams.getFirst("principal");
        tokenRequestParameter.principalId = formParams.getFirst("principal_id");
        tokenRequestParameter.scope = formParams.getFirst("scope");
        tokenRequestParameter.personId = formParams.getFirst("person_id");

        // AC flow only
        tokenRequestParameter.code = formParams.getFirst("code");
        tokenRequestParameter.clientAssertionType = formParams.getFirst("client_assertion_type");
        tokenRequestParameter.clientAssertion = formParams.getFirst("client_assertion");

        log2Console(headers, formParams);

        if (!tokenRequestParameter.isComplete()) {
            LOG.error("Invalid request. At least one required parameter is missing for grant type = " + tokenRequestParameter.grantType);
            return Response.status(400, "invalid_request").build();
        }

        if (GrantType.clientCredentials.equals(tokenRequestParameter.grantType) ||
                GrantType.authorizationCode.equals(tokenRequestParameter.grantType)) {

            // build JWT depending on the grant type
            String jwt = authorizationService.buildResponse(tokenRequestParameter);
            return Response.ok(jwt).build();

        } else {
            return Response.status(400, "unsupported_grant_type").build();
        }
    }

    private static void log2Console(HttpHeaders headers, MultivaluedMap<String, String> formParam) {

        LOG.info("Incoming request:");
        LOG.info("Header data:");

        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        requestHeaders.keySet().forEach(
                key -> LOG.info(key + ":" + requestHeaders.get(key))
        );

        LOG.info("Form parameters:");

        formParam.keySet().forEach(key -> LOG.info(key + ":" + formParam.get(key)));

    }

    private List<String> parseAuthorizationHeader(String authHeader) {

        // Check if the header starts with "Basic "
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Extract the Base64 encoded part
            String base64Credentials = authHeader.substring("Basic ".length()).trim();

            // Decode the Base64 string
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));

            // Split the credentials into client_id and client_secret
            String[] values = credentials.split(":", 2);
            String clientId = values[0];
            String clientSecret = values[1];

            // Output the results
            LOG.info("Client ID: " + clientId);
            LOG.info("Client Secret: " + clientSecret);

            return List.of(clientId, clientSecret);

        } else {
            LOG.info("Invalid Authorization header");
            return List.of();
        }

    }

}
