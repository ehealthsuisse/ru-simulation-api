package org.fnm.simulator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the simulator against the Simulation Service API contract, on the four points a
 * simulation client depends on:
 * <ul>
 *   <li>the sequences checksum is a Checksum object, not a bare number;</li>
 *   <li>setup answers 202 Accepted with additional instructions;</li>
 *   <li>the {@code simulationId} in those instructions is what resume accepts;</li>
 *   <li>the report is POSTed to the callback URL supplied at setup.</li>
 * </ul>
 * Most of this is the Gazelle {@code SimulationController} and simulation manager rather than code
 * of this simulator; the tests pin the wiring, so that a change to it cannot quietly break the contract.
 */
@QuarkusTest
class SimulationApiConformanceTest {

    private static final String SEQUENCE_ID = HelloGazelleSimulationService.SEQUENCE_ID;

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

    private static String setupRequest(String message) {
        return """
                {
                  "sequenceId": "%s",
                  "simulationParameters": [
                    { "name": "message", "type": "TEXT", "value": "%s" }
                  ],
                  "timeoutSeconds": 30
                }""".formatted(SEQUENCE_ID, message);
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
                .body(setupRequest("Hello from the conformance test"))
                .queryParam("callback", callbackUrl("session-1"))
                .when().post("/simulation/v1/simulations");

        setup.then()
                .statusCode(202)
                .body("outcome", org.hamcrest.Matchers.is("additional-instructions"))
                .body("simulationId", notNullValue());

        String simulationId = setup.jsonPath().getString("simulationId");
        assertNotNull(simulationId);
        assertTrue(!simulationId.equals(SEQUENCE_ID),
                "simulationId must identify the session, not the sequence");

        given()
                .contentType(ContentType.JSON)
                .when().post("/simulation/v1/simulations/{id}/resume", simulationId)
                .then()
                .statusCode(201)
                .body("outcome", org.hamcrest.Matchers.is("switch-to-execution"));

        String query = receivedQueries.poll(10, TimeUnit.SECONDS);
        String report = receivedReports.poll(1, TimeUnit.SECONDS);

        assertEquals("session=session-1", query,
                "the report has to go to the callback URL supplied at setup, unaltered");
        assertNotNull(report, "a simulation report is expected on the callback");
        assertTrue(report.contains("\"result\":\"PASSED\""), report);
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
