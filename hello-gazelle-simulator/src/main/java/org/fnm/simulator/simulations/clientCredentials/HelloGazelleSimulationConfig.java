package org.fnm.simulator.simulations.clientCredentials;

import net.ihe.gazelle.simulation.business.callback.Role;
import net.ihe.gazelle.simulation.business.setup.*;

import java.util.List;

/**
 * Container for the simulation configuration and parameters.
 */
public class HelloGazelleSimulationConfig {

    // fixed for IUA client simulation
    public final Role initiator;
    public final Role responder;

    // parameter read from setup indicating the current test session
    public String sessionId;

    // the identity of the sequence supported by the simulation
    public String sequenceId;

    public long timeoutInSeconds;

    // parameters read from setup
    public List<Parameter> simulationParameters;

    // parameter read from setup
    public String message;

    /**
     * Container for the simulation configuration parameters.
     *
     * @param sessionId the current test session
     * @param simulationRequest the information required for a single simulation run
     */
    public HelloGazelleSimulationConfig(String sessionId, SimulationRequest simulationRequest) {

        this.sessionId = sessionId;
        this.sequenceId = simulationRequest.getSequenceId();
        this.simulationParameters = simulationRequest.getSimulationParameters();

        this.timeoutInSeconds = simulationRequest.getTimeoutSeconds();

        initiator = new Role();
        initiator.setName("Hello Gazelle Simulator");
        initiator.setConfigs(List.of());
        initiator.setSimulated(true);

        responder = new Role();
        responder.setName("System console");
        responder.setConfigs(List.of());

        // parse parameters
        for (Parameter parameter : simulationParameters) {

            String name = parameter.getName();
            ParameterType type = parameter.getType();

            if ((type != null) && type.equals(ParameterType.TEXT)){
                if (name.equals("message")) {
                    message = parameter.getValue();
                }
            }
        }
    }


}
