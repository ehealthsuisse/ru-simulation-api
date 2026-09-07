package org.fnm.simulator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.fnm.simulator.helper.SigningKeyHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks the simulator against the Simulation Service API contract, on the four points a
 * simulation client depends on:
 * <ul>
 *   <li>the sequences checksum is a Checksum object, not a bare number;</li>
 *   <li>setup answers 202 Accepted with additional instructions;</li>
 *   <li>the {@code simulationId} in those instructions is what resume accepts;</li>
 *   <li>the report is POSTed to the callback URL supplied at setup.</li>
 * </ul>
 * The token endpoint is not reachable in this test, so the run reports a failed transaction --
 * which is the point: the report still has to reach the callback that was supplied.
 */
@QuarkusTest
class SimulationApiConformanceTest {

    private static final String CC_SEQUENCE_ID = IUAClientSimulationService.CLIENT_CREDENTIAL_SEQUENCE_ID;

    /** Stands in for the test environment's report endpoint. */
    private HttpServer callbackSink;
    private final BlockingQueue<String> receivedQueries = new ArrayBlockingQueue<>(4);
    private final BlockingQueue<String> receivedReports = new ArrayBlockingQueue<>(4);

    @BeforeEach
    void startCallbackSink() throws IOException {
        callbackSink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        callbackSink.createContext("/simulation/v1/report", this::receiveReport);
        callbackSink.start();
    }

    @AfterEach
    void stopCallbackSink() {
        callbackSink.stop(0);
    }

    private void receiveReport(HttpExchange exchange) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            receivedReports.add(new String(body.readAllBytes(), StandardCharsets.UTF_8));
        }
        receivedQueries.add(String.valueOf(exchange.getRequestURI().getQuery()));
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private String callbackUrl(String session) {
        return "http://127.0.0.1:" + callbackSink.getAddress().getPort()
                + "/simulation/v1/report?session=" + session;
    }

    /** A client-credentials setup carrying every parameter the sequence declares as required. */
    private static String clientCredentialsRequest() {
        return """
                {
                  "sequenceId": "%s",
                  "simulationParameters": [
                    { "name": "token_endpoint_url", "type": "TEXT", "value": "http://127.0.0.1:1/token" },
                    { "name": "client_id",          "type": "TEXT", "value": "client-id" },
                    { "name": "client_secret",      "type": "TEXT", "value": "client-secret" },
                    { "name": "scope",              "type": "TEXT", "value": "purpose_of_use=urn:oid:2.16.756.5.30.1.127.3.10.5|AUTO subject_role=urn:oid:2.16.756.5.30.1.127.3.10.6|TC" },
                    { "name": "principal",          "type": "TEXT", "value": "principal.name" },
                    { "name": "principal_id",       "type": "TEXT", "value": "7601000000000" },
                    { "name": "jwt_public_key",     "type": "TEXT", "value": %s }
                  ],
                  "timeoutSeconds": 30
                }""".formatted(CC_SEQUENCE_ID, jsonString(SigningKeyHelper.getRSAPublicKey()));
    }

    /** Minimal JSON string escaping -- the public key is itself a JSON document. */
    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    @Test
    void checksumIsAChecksumObjectInHexadecimal08Format() {
        given()
                .when().get("/simulation/v1/sequences/checksum")
                .then()
                .statusCode(200)
                .body("checksum", notNullValue())
                .body("checksum", matchesPattern("0x[0-9A-F]{8}"));
    }

    @Test
    void setupAnswersAcceptedAndTheSimulationIdResumes() throws InterruptedException {
        Response setup = given()
                .contentType(ContentType.JSON)
                .body(clientCredentialsRequest())
                .queryParam("callback", callbackUrl("session-1"))
                .when().post("/simulation/v1/simulations");

        setup.then()
                .statusCode(202)
                .body("outcome", is("additional-instructions"))
                .body("simulationId", notNullValue());

        String simulationId = setup.jsonPath().getString("simulationId");
        assertNotEquals(CC_SEQUENCE_ID, simulationId,
                "simulationId must identify the session, not the sequence");

        given()
                .contentType(ContentType.JSON)
                .when().post("/simulation/v1/simulations/{id}/resume", simulationId)
                .then()
                .statusCode(201)
                .body("outcome", is("switch-to-execution"));

        String query = receivedQueries.poll(30, TimeUnit.SECONDS);
        String report = receivedReports.poll(1, TimeUnit.SECONDS);

        assertEquals("session=session-1", query,
                "the report has to go to the callback URL supplied at setup, unaltered");
        assertNotNull(report, "a simulation report is expected on the callback");
    }

    @Test
    void resumeOfAnUnknownSimulationIsNotFound() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/simulation/v1/simulations/{id}/resume", "no-such-session")
                .then()
                .statusCode(404);
    }
}
