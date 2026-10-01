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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the simulator against the Simulation Service API contract, on the points a simulation
 * client depends on:
 * <ul>
 *   <li>the sequences checksum is a Checksum object, not a bare number;</li>
 *   <li>setup answers 202 Accepted with additional instructions;</li>
 *   <li>the {@code simulationId} in those instructions is what resume accepts;</li>
 *   <li>the report is POSTed to the callback URL supplied at setup;</li>
 *   <li>a setup that cannot run is refused with 400 Bad Request;</li>
 *   <li>the timeout requested at setup bounds the whole run, and a timed-out run is reported once.</li>
 * </ul>
 * Most of this is the Gazelle {@code SimulationController} and simulation manager rather than code
 * of this simulator; the tests pin the wiring, so that a change to it cannot quietly break the contract.
 */
@QuarkusTest
class SimulationApiConformanceTest {

    private static final String CC_SEQUENCE_ID = IUAClientSimulationService.CLIENT_CREDENTIAL_SEQUENCE_ID;
    private static final String AC_SEQUENCE_ID = IUAClientSimulationService.AUTHORIZATION_CODE_SEQUENCE_ID;

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

    /** An authorization-code setup against the given authorization server, with every required parameter. */
    private static String authorizationCodeRequest(String authorizationServer, int timeoutSeconds) {
        return """
                {
                  "sequenceId": "%s",
                  "simulationParameters": [
                    { "name": "code_endpoint_url",  "type": "TEXT", "value": "%s/authorize" },
                    { "name": "token_endpoint_url", "type": "TEXT", "value": "%s/token" },
                    { "name": "client_id",          "type": "TEXT", "value": "client-id" },
                    { "name": "client_secret",      "type": "TEXT", "value": "client-secret" },
                    { "name": "scope",              "type": "TEXT", "value": "purpose_of_use=urn:oid:2.16.756.5.30.1.127.3.10.5|NORMAL subject_role=urn:oid:2.16.756.5.30.1.127.3.10.6|HCP" },
                    { "name": "jwt_public_key",     "type": "TEXT", "value": %s }
                  ],
                  "timeoutSeconds": %d
                }""".formatted(AC_SEQUENCE_ID, authorizationServer, authorizationServer,
                jsonString(SigningKeyHelper.getRSAPublicKey()), timeoutSeconds);
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

    @Test
    void setupMissingARequiredParameterIsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(clientCredentialsRequest().replace("\"client_id\"", "\"not_client_id\""))
                .queryParam("callback", callbackUrl("session-3"))
                .when().post("/simulation/v1/simulations")
                .then()
                .statusCode(400)
                .body(containsString("Client id is not set."));
    }

    @Test
    void timeoutBoundsTheWholeRunAndIsReportedOnce() throws Exception {
        // Each of the two answers comes within the 2 second timeout, both together do not: the run
        // has to be cancelled while it waits for the token.
        HttpServer authorizationServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();
        authorizationServer.setExecutor(handlers);
        authorizationServer.createContext("/authorize", exchange -> {
            pause(1200);
            exchange.getResponseHeaders().add("Location", "https://0.0.0.0:8080/callback?code=code-1&state=123456789");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        authorizationServer.createContext("/token", exchange -> {
            pause(1500);
            exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        authorizationServer.start();

        try {
            String baseUrl = "http://127.0.0.1:" + authorizationServer.getAddress().getPort();
            Response setup = given()
                    .contentType(ContentType.JSON)
                    .body(authorizationCodeRequest(baseUrl, 2))
                    .queryParam("callback", callbackUrl("session-2"))
                    .when().post("/simulation/v1/simulations");
            setup.then().statusCode(202);

            given()
                    .contentType(ContentType.JSON)
                    .when().post("/simulation/v1/simulations/{id}/resume", setup.jsonPath().getString("simulationId"))
                    .then()
                    .statusCode(201);

            String report = receivedReports.poll(10, TimeUnit.SECONDS);
            assertNotNull(report, "a simulation that timed out still has to be reported");
            assertTrue(report.contains("Simulation Timeout"), "expected a timeout report -- got: " + report);

            // by now the token request would have been answered, and the run would have reported its failure
            assertNull(receivedReports.poll(3, TimeUnit.SECONDS),
                    "the cancelled run must not send a report of its own after the timeout report");
        } finally {
            authorizationServer.stop(0);
            handlers.shutdownNow();
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
