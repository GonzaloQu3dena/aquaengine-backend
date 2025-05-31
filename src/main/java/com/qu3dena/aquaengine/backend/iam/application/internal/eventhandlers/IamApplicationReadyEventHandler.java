package com.qu3dena.aquaengine.backend.iam.application.internal.eventhandlers;

import com.qu3dena.aquaengine.backend.iam.domain.model.commands.SeedRolesCommand;
import com.qu3dena.aquaengine.backend.iam.domain.services.RoleCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/**
 * Event handler that seeds roles when the application is ready.
 * <p>
 * Listens for the {@link ApplicationReadyEvent} and triggers the seeding of roles
 * using the {@link RoleCommandService} if necessary.
 * </p>
 */
@Service
public class IamApplicationReadyEventHandler {

    private final RoleCommandService roleCommandService;
    private static final Logger LOGGER = LoggerFactory.getLogger(IamApplicationReadyEventHandler.class);

    /**
     * Constructs the event handler with the required {@link RoleCommandService}.
     *
     * @param roleCommandService the service used to seed roles
     */
    public IamApplicationReadyEventHandler(RoleCommandService roleCommandService) {
        this.roleCommandService = roleCommandService;
    }

    /**
     * Handles the {@link ApplicationReadyEvent} by seeding roles if needed.
     *
     * @param event the application ready event
     */
    @EventListener
    public void on(ApplicationReadyEvent event) {
        var applicationName = event.getApplicationContext().getId();
        LOGGER.info("Starting to verify if roles seeding is needed for {} at {}", applicationName, currentTimestamp());
        var seedRolesCommand = new SeedRolesCommand();
        roleCommandService.handle(seedRolesCommand);
        LOGGER.info("Roles seeding verification finished for {} at {}", applicationName, currentTimestamp());
    }

    /**
     * Returns the current timestamp.
     *
     * @return the current {@link Timestamp}
     */
    private Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}