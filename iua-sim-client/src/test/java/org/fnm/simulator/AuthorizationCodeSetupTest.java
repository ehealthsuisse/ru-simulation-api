package org.fnm.simulator;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.fnm.simulator.helper.SigningKeyHelper;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The authorization code sequence declares {@code basic_auth_user} and {@code basic_auth_password}
 * as required parameters and uses them to build the HTTP Basic header of the authorization request.
 * A setup that supplies them has to be accepted.
 */
@QuarkusTest
class AuthorizationCodeSetupTest {

    private static final String AC_SEQUENCE_ID = IUAClientSimulationService.AUTHORIZATION_CODE_SEQUENCE_ID;

    /** An authorization-code setup carrying every parameter the sequence declares as required. */
    private static String authorizationCodeRequest() {
        return """
                {
                  "sequenceId": "%s",
                  "simulationParameters": [
                    { "name": "code_endpoint_url",   "type": "TEXT", "value": "http://127.0.0.1:1/authorize" },
                    { "name": "basic_auth_user",     "type": "TEXT", "value": "basic-auth-user" },
                    { "name": "basic_auth_password", "type": "TEXT", "value": "basic-auth-password" },
                    { "name": "token_endpoint_url",  "type": "TEXT", "value": "http://127.0.0.1:1/token" },
                    { "name": "client_id",           "type": "TEXT", "value": "client-id" },
                    { "name": "client_secret",       "type": "TEXT", "value": "client-secret" },
                    { "name": "scope",               "type": "TEXT", "value": "purpose_of_use=urn:oid:2.16.756.5.30.1.127.3.10.5|NORMAL subject_role=urn:oid:2.16.756.5.30.1.127.3.10.6|HCP" },
                    { "name": "jwt_public_key",      "type": "TEXT", "value": %s }
                  ],
                  "timeoutSeconds": 30
                }""".formatted(AC_SEQUENCE_ID, jsonString(SigningKeyHelper.getRSAPublicKey()));
    }

    /** Minimal JSON string escaping -- the public key is itself a JSON document. */
    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    @Test
    void setupAcceptsTheSuppliedBasicAuthenticationCredentials() {
        Response setup = given()
                .contentType(ContentType.JSON)
                .body(authorizationCodeRequest())
                .queryParam("callback", "http://127.0.0.1:1/simulation/v1/report?session=session-1")
                .when().post("/simulation/v1/simulations");

        String instruction = String.valueOf(setup.jsonPath().getString("instruction"));

        assertFalse(instruction.contains("http basic authentication is not set"),
                "basic_auth_user and basic_auth_password were supplied, so setup must not "
                        + "report them missing -- got: " + instruction);
        assertTrue(instruction.contains("initialized and can be started"),
                "expected the sequence to be set up -- got: " + instruction);
    }
}
