package org.fnm.simulator.simulations.clientCredentials;

import net.ihe.gazelle.simulation.business.callback.Result;
import net.ihe.gazelle.simulation.business.callback.TransactionReport;
import org.fnm.simulator.simulations.Status;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * The simulation entry point. Creates the configuration and action objects and runs the simulation.
 */
public class HelloGazelleSimulation {

    private static final Logger LOG = Logger.getLogger(HelloGazelleSimulation.class);

    public Status status = Status.READY;
    public final Instant createdAt = Instant.now();

    private final HelloGazelleSimulationConfig config;
    private final HelloGazelleSimulationAction action;

    /**
     * Constructor with configuration parameters.
     *
     * @param config the configuration parameters for the client credential flow.
     */
    public HelloGazelleSimulation(HelloGazelleSimulationConfig config) {
        this.config = config;
        this.action = new HelloGazelleSimulationAction(config);
    }

    /**
     * Run the simulation.
     *
     * @return transaction report indicating the result of the test.
     */
    public TransactionReport run() {

        status = Status.RUNNING;
        LOG.info("Running ClientCredentialsSimulation with session id " + config.sessionId);

        try {
            return action.run();
        } catch (Exception e) {
            status = Status.DONE;
            LOG.error("Exception in running the simulation ", e);
            return new TransactionReport().setResult(Result.FAILED).setNote("Exception in running the simulation " + e.getMessage());
        } finally {
            status = Status.DONE;
        }
    }

    public HelloGazelleSimulationConfig getConfig() {
        return config;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}

