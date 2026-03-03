package net.unit8.bouncr.api.resource;

import tools.jackson.core.type.TypeReference;
import enkan.collection.Parameters;
import kotowari.restful.Decision;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.entity.Invitation;
import net.unit8.bouncr.sign.JsonWebToken;

import jakarta.inject.Inject;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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

    @Decision(EXISTS)
    public boolean exists(Parameters params, RestContext context, EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Invitation> query = cb.createQuery(Invitation.class);
        Root<Invitation> invitationRoot = query.from(Invitation.class);
        query.where(cb.equal(invitationRoot.get("code"), params.get("code")));
        Invitation invitation = em.createQuery(query)
                .setHint("jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH)
                .getResultStream().findAny().orElse(null);
        if (invitation != null) {
            context.putValue(invitation);
        }
        return invitation != null;
    }

    @Decision(HANDLE_OK)
    public Map<String, Object> invitation(Invitation invitation) {
        Map<String, Object> invitationResponse = new HashMap<>();
        Optional.ofNullable(invitation.getOidcInvitations())
                .filter(oidcInvitations -> !oidcInvitations.isEmpty())
                .map(oidcInvitations -> oidcInvitations.stream()
                        .map(oidcInvitation -> {
                            return Map.of(
                                    "oidc_provider", oidcInvitation.getOidcProvider().getName(),
                                    "claim", jsonWebToken.decodePayload(oidcInvitation.getOidcPayload(),
                                            new TypeReference<Map<String, Object>>() {})
                            );
                        })
                        .collect(Collectors.toList())
                )
                .ifPresent(oidcInvitations -> invitationResponse.put("oidc_invitations", oidcInvitations));

        Optional.ofNullable(invitation.getGroupInvitations())
                .filter(groupInvitations -> !groupInvitations.isEmpty())
                .map(groupInvitations -> groupInvitations.stream()
                        .map(groupInvitation -> groupInvitation.getGroup().getName())
                        .collect(Collectors.toList()))
                .ifPresent(groupInvitations -> invitationResponse.put("group_invitations", groupInvitations));

        return invitationResponse;
    }
}
