package org.fnm.simulator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import net.ihe.gazelle.simulation.business.sequence.SimulationChecksumService;
import net.ihe.gazelle.simulation.jaxrs.server.business.SimulationManager;
import net.ihe.gazelle.simulation.jaxrs.server.technical.SimulationChecksumServiceImpl;
import net.ihe.gazelle.simulation.jaxrs.server.technical.SimulationManagerFactory;

/**
 * Wires {@link HelloGazelleSimulationService} into the Gazelle simulation framework.
 * <p>
 * The simulation manager owns the session lifecycle: it generates the session id, runs each
 * simulation on a thread of its own, cancels it once the timeout requested at setup has passed and
 * reports that timeout to the callback URL supplied at setup.
 */
@ApplicationScoped
public class HelloGazelleSimulationManagerFactory extends SimulationManagerFactory {

    @Inject
    HelloGazelleSimulationService simulationService;

    @Inject
    HelloGazelleMetadataService metadataService;

    @Produces
    @ApplicationScoped
    SimulationManager simulationManager() {
        return createManager(simulationService, metadataService);
    }

    @Produces
    @ApplicationScoped
    SimulationChecksumService simulationChecksumService() {
        return new SimulationChecksumServiceImpl();
    }
}
