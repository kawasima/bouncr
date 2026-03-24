package net.unit8.bouncr.api.boundary;

import java.util.List;

/**
 * Request body for creating an invitation.
 *
 * @param email email address of the invitee
 * @param groups list of groups to assign the invitee to
 */
public record InvitationCreate(String email, List<IdObject> groups) {}
