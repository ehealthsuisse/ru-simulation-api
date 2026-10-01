package org.fnm.simulator;

import jakarta.enterprise.context.ApplicationScoped;
import net.ihe.gazelle.servicemetadata.api.business.MetadataService;
import net.ihe.gazelle.servicemetadata.api.business.Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Name and version of the simulation service, as stated in every simulation report -- those the
 * simulator sends and those the simulation manager sends on its behalf, such as a timeout.
 */
@ApplicationScoped
public class IUAClientMetadataService implements MetadataService {

    private static final String SERVICE_NAME = "IUAClientSimulationService";

    @ConfigProperty(name = "version")
    String version;

    @Override
    public Service getMetadata() {
        return new Service()
                .setName(SERVICE_NAME)
                .setVersion(version);
    }
}
