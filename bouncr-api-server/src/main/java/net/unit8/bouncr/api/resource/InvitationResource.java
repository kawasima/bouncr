package net.unit8.bouncr.api.resource;

import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.ContextKey;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.repository.InvitationRepository;
import net.unit8.bouncr.data.Invitation;
import net.unit8.bouncr.sign.JsonWebToken;
import org.jooq.DSLContext;
import tools.jackson.core.type.TypeReference;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static kotowari.restful.DecisionPoint.EXISTS;
import static kotowari.restful.DecisionPoint.HANDLE_OK;

@AllowedMethods({"GET"})
public class InvitationResource {
    @Inject
    private JsonWebToken jsonWebToken;

    static final ContextKey<Invitation> INVITATION = ContextKey.of(Invitation.class);

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, DSLContext dsl) {
        InvitationRepository repo = new InvitationRepository(dsl);
        Optional<Invitation> invitation = repo.findByCode(params.get("code"));
        invitation.ifPresent(i -> context.put(INVITATION, i));
        return invitation.isPresent();
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> invitation(Invitation invitation) {
        Map<String, Object> invitationResponse = new HashMap<>();
        Optional.ofNullable(invitation.oidcInvitations())
                .filter(oidcInvitations -> !oidcInvitations.isEmpty())
                .map(oidcInvitations -> oidcInvitations.stream()
                        .map(oidcInvitation -> {
                            return Map.of(
                                    "oidc_provider", oidcInvitation.oidcProvider().name(),
                                    "claim", jsonWebToken.decodePayload(oidcInvitation.oidcPayload(),
                                            new TypeReference<Map<String, Object>>() {})
                            );
                        })
                        .collect(Collectors.toList())
                )
                .ifPresent(oidcInvitations -> invitationResponse.put("oidc_invitations", oidcInvitations));

        Optional.ofNullable(invitation.groupInvitations())
                .filter(groupInvitations -> !groupInvitations.isEmpty())
                .map(groupInvitations -> groupInvitations.stream()
                        .map(groupInvitation -> groupInvitation.group().name())
                        .collect(Collectors.toList()))
                .ifPresent(groupInvitations -> invitationResponse.put("group_invitations", groupInvitations));

        return invitationResponse;
    }
}
