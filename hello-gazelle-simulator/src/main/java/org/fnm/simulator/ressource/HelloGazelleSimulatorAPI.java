package org.fnm.simulator.ressource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import net.ihe.gazelle.simulation.business.sequence.SimulationChecksumService;
import net.ihe.gazelle.simulation.business.sequence.SimulationSequenceService;
import net.ihe.gazelle.simulation.jaxrs.server.business.SimulationManager;
import net.ihe.gazelle.simulation.jaxrs.server.technical.SimulationController;

/**
 * The Simulation Service API endpoints.
 * <p>
 * The wire contract -- status codes, the checksum object, session ids, the report POSTed to the
 * callback URL and the simulation timeout -- is the Gazelle {@link SimulationController}'s. What this
 * simulator does is in {@link org.fnm.simulator.HelloGazelleSimulationService}.
 */
@Path("")
public class HelloGazelleSimulatorAPI extends SimulationController {

    @Inject
    public HelloGazelleSimulatorAPI(SimulationChecksumService simulationChecksumService,
                                    SimulationSequenceService simulationSequenceService,
                                    SimulationManager simulationManager) {
        super(simulationChecksumService, simulationSequenceService, simulationManager);
    }
}
