package net.unit8.bouncr.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.ContentNegotiable;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.component.config.HookPoint;
import net.unit8.bouncr.sign.JsonWebToken;
import net.unit8.bouncr.sign.JwtClaim;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.InvitationDao;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.SignUpForm;
import net.unit8.bouncr.web.service.SignInService;
import net.unit8.bouncr.web.service.UserValidationService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;

public class SignUpController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private StoreProvider storeProvider;

    @Inject
    private BouncrConfiguration config;

    @Inject
    private JsonWebToken jsonWebToken;

    private SignInService signInService;
    private UserValidationService userValidationService;

    @PostConstruct
    public void initialize() {
        signInService = new SignInService(daoProvider, storeProvider, config);
        userValidationService = new UserValidationService(daoProvider, config);
    }

    public HttpResponse newForm(Parameters params) {
        String code = params.get("code");
        List<GroupInvitation> groupInvitations = Collections.emptyList();
        OidcInvitation oidcInvitation = null;

        SignUpForm form = new SignUpForm();
        form.setCode(code);

        if (code != null) {
            InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
            Invitation invitation = invitationDao.selectByCode(code);
            groupInvitations = invitationDao.selectGroupInvitations(invitation.getId());
            oidcInvitation = invitationDao.selectOidcInvitation(invitation.getId());
            JwtClaim claim = jsonWebToken.decodePayload(oidcInvitation.getOidcPayload(), new TypeReference<JwtClaim>() {});
            form.setName(claim.getName());
            form.setEmail(claim.getEmail());
        }
        return templateEngine.render("my/signUp/new",
                "signUp", form,
                "passwordEnabled", config.isPasswordEnabled(),
                "groupInvitations", groupInvitations,
                "oidcInvitation", oidcInvitation);
    }

    @Transactional
    public HttpResponse create(SignUpForm form, HttpRequest request) {
        userValidationService.validate(form, ContentNegotiable.class.cast(request).getLocale());
        if (form.hasErrors()) {
            List<GroupInvitation> groupInvitations = Collections.emptyList();
            List<OidcInvitation> oidcInvitations = Collections.emptyList();
            if (form.getCode() != null && !form.getCode().isEmpty()) {
                InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                Invitation invitation = invitationDao.selectByCode(form.getCode());
                invitationDao.selectGroupInvitations(invitation.getId());
                invitationDao.selectOidcInvitation(invitation.getId());
            }
            return templateEngine.render("my/signUp/new",
                    "signUp", form,
                    "passwordEnabled", config.isPasswordEnabled(),
                    "groupInvitations", groupInvitations,
                    "oidcInvitations", oidcInvitations);
        } else {
            UserDao userDao = daoProvider.getDao(UserDao.class);

            User user = beansConverter.createFrom(form, User.class);
            user.setWriteProtected(false);
            userDao.insert(user);
            GroupDao groupDao = daoProvider.getDao(GroupDao.class);
            Group bouncrUserGroup = groupDao.selectByName("BOUNCR_USER");
            groupDao.addUser(bouncrUserGroup, user);

            if (config.isPasswordEnabled() && !form.isPasswordDisabled()) {
                PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
                String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
                passwordCredentialDao.insert(builder(new PasswordCredential())
                        .set(PasswordCredential::setId, user.getId())
                        .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getPassword(), salt, 100))
                        .set(PasswordCredential::setSalt, salt)
                        .set(PasswordCredential::setInitial, false)
                        .build());
            }


            if (!form.getCode().isEmpty()) {
                InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                Invitation invitation = invitationDao.selectByCode(form.getCode());
                if (invitation == null) {
                    return templateEngine.render("my/signUp/new",
                            "passwordEnabled", config.isPasswordEnabled(),
                            "signUp", form,
                            "groupInvitations", Collections.emptyList(),
                            "oidcInvitations", Collections.emptyList());
                }
                invitationDao.selectGroupInvitations(invitation.getId())
                        .stream()
                        .forEach(groupInvitation -> {
                            Group group = groupDao.selectById(groupInvitation.getGroupId());
                            groupDao.addUser(group, user);
                        });

                Optional.ofNullable(invitationDao.selectOidcInvitation(invitation.getId()))
                        .ifPresent(oidcInvitation -> userDao.connectToOidcProvider(user.getId(), oidcInvitation.getOidcProviderId(),
                                jsonWebToken.decodePayload(oidcInvitation.getOidcPayload(), new TypeReference<JwtClaim>() {}).getSub()));
                invitationDao.delete(invitation);
            }

            String token = signInService.signIn(user, request);

            config.getHookRepo().runHook(HookPoint.AFTER_SIGNUP, user);
            return signInService.responseSignedIn(token, request, "/my");
        }
    }
}
