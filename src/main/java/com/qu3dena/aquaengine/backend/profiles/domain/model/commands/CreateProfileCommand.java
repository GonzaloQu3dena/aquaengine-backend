package com.qu3dena.aquaengine.backend.profiles.domain.model.commands;

/**
 * Command object for creating a new user profile.
 * <p>
 * Encapsulates all the data required to create a profile.
 * </p>
 *
 * @param userId         Unique user identifier.
 * @param firstName      User's first name.
 * @param lastName       User's last name.
 * @param contactEmail   User's contactEmail address.
 * @param contactPhone   User's contactPhone number.
 * @param companyName    Company name.
 * @param companyStreet  Company street address.
 * @param companyCity    Company city.
 * @param postalCode     Company postal code.
 * @param companyCountry Company country.
 */
public record CreateProfileCommand(
        Long userId,
        String firstName,
        String lastName,
        String contactEmail,
        String contactPhone,
        String companyName,
        String companyStreet,
        String companyCity,
        String postalCode,
        String companyCountry
) {
}