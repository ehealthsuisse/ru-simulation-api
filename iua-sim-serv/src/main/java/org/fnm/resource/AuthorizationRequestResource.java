package org.fnm.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.fnm.AuthorizationService;
import org.fnm.model.AuthorizationRequestParameter;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Path("/authorize")
public class AuthorizationRequestResource {

    private static final Logger LOG = Logger.getLogger(AuthorizationRequestResource.class);

    @Inject
    AuthorizationService authorizationService;

    @GET
    public Response get(@Context HttpHeaders headers, @Context UriInfo uriInfo) {

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        // just to know
        log2Console(headers, queryParams);

        AuthorizationRequestParameter requestParameter = new AuthorizationRequestParameter();
        requestParameter.responseType = queryParams.getFirst("response_type");
        requestParameter.clientId = queryParams.getFirst("client_id");
        requestParameter.state = queryParams.getFirst("state");
        requestParameter.redirectUri = queryParams.getFirst("redirect_uri");
        requestParameter.scope = queryParams.getFirst("scope");
        requestParameter.personId = queryParams.getFirst("person_id");
        requestParameter.principal = queryParams.getFirst("principal");
        requestParameter.principalId = queryParams.getFirst("principal_id");
        requestParameter.group = queryParams.getFirst("group");
        requestParameter.groupId = queryParams.getFirst("group_id");
        requestParameter.resource = queryParams.getFirst("resource");
        requestParameter.codeChallenge = queryParams.getFirst("code_challenge");
        requestParameter.codeChallengeMethod = queryParams.getFirst("code_challenge_method");
        requestParameter.requestedTokenType = queryParams.getFirst("requested_token_type");

        // get client id and secret from authorization header
        List<String> clientIdAndSecret =
                parseAuthorizationHeader(headers.getRequestHeaders().get("Authorization").getFirst());
        requestParameter.clientId = clientIdAndSecret.getFirst(); // client id is read twice, but it's ok'
        requestParameter.clientSecret = clientIdAndSecret.getLast();

        if (!requestParameter.isComplete()) {
            LOG.error("Invalid request. At least one required parameter is missing.");
            return Response.status(400, "Invalid request. At least one required parameter is missing.").build();
        }

        // generate authorization code
        String code = UUID.randomUUID().toString();
        String uri = requestParameter.redirectUri + "?code=" + code + "&state=" + requestParameter.state;

        // register authorization request data at authorization service
        authorizationService.registerAuthorizationRequest(code, requestParameter);
        LOG.info("Authorization request registered with code = " + code);

        // re-direct to redirectUri. Uses http code 302 which allows the user agent to switch to the
        // http POST protocol to send the code and state to the applications callback endpoint.
        return Response.status(302).location(URI.create(uri)).build();
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

    private static void log2Console(HttpHeaders headers, MultivaluedMap<String, String> queryParam) {

        LOG.info("Incoming request:");
        LOG.info("Header data:");

        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        requestHeaders.keySet().forEach(
                key -> LOG.info(key + ":" + requestHeaders.get(key))
        );

        LOG.info("Incoming request with query parameters:");
        queryParam.keySet().forEach(
                key -> LOG.info(key + ":" + queryParam.get(key))
        );
    }

}
