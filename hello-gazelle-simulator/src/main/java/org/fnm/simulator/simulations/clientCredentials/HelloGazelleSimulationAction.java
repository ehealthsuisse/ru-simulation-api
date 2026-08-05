package org.fnm.simulator.simulations.clientCredentials;

import net.ihe.gazelle.simulation.business.callback.*;
import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.List;

/**
 * Represents one action of the simulation.
 */
public class HelloGazelleSimulationAction {

    private static final Logger LOG = Logger.getLogger(HelloGazelleSimulationAction.class);

    // the simulation parameters
    private final HelloGazelleSimulationConfig config;

    /**
     * Constructor with configuration parameters.
     *
     * @param config the configuration parameters for the client credential flow.
     */
    public HelloGazelleSimulationAction(HelloGazelleSimulationConfig config) {
        this.config = config;
    }

    /**
     * Run the simulation.
     *
     * @return TransactionReport indicating the result of the test.
     */
    public TransactionReport run() {

        String message = config.message;

        // perform the hello gazelle action
        LOG.info(message);

        // create the transaction report
        TransactionReport report = new TransactionReport();
        report.setResult(Result.PASSED);
        report.setStandards(List.of("UTF-8, org.jboss.logging"));
        report.setInitiator(config.initiator);
        report.setResponder(config.responder);
        report.setTransaction("LOG to console.");
        report.setStandards(List.of("UTF-8, org.jboss.logging"));

        Message requestMessage = new Message();
        requestMessage.setName("Log to console");
        requestMessage.setContent(message.getBytes());
        requestMessage.setDateTime(Instant.now());
        requestMessage.setSender(config.initiator.getName());
        requestMessage.setReceiver(config.responder.getName());

        report.setMessages(List.of(requestMessage));

        report.setNote("Successfully logged message to console.");
        return report;
    }

}
