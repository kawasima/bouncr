package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.component.StoreProvider;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.InvitationDao;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.SignUpForm;
import net.unit8.bouncr.web.service.SignInService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;

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

    private SignInService signInService;

    @PostConstruct
    public void initialize() {
        signInService = new SignInService(daoProvider, storeProvider, config);
    }

    public HttpResponse newForm(Parameters params) {
        String code = params.get("code");
        List<GroupInvitation> groupInvitations = Collections.emptyList();
        List<OidcInvitation> oidcInvitations = Collections.emptyList();
        if (code != null) {
            InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
            Invitation invitation = invitationDao.selectByCode(code);
            groupInvitations = invitationDao.selectGroupInvitations(invitation.getId());
            oidcInvitations = invitationDao.selectOidcInvitations(invitation.getId());
        }
        SignUpForm form = new SignUpForm();
        form.setCode(code);
        return templateEngine.render("my/signUp/new",
                "signUp", form,
                "passwordEnabled", config.isPasswordEnabled(),
                "groupInvitations", groupInvitations,
                "oidcInvitations", oidcInvitations);
    }

    @Transactional
    public HttpResponse create(SignUpForm form, HttpRequest request) {
        if (form.hasErrors()) {
            List<GroupInvitation> groupInvitations = Collections.emptyList();
            List<OidcInvitation> oidcInvitations = Collections.emptyList();
            if (form.getCode() != null && !form.getCode().isEmpty()) {
                InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                Invitation invitation = invitationDao.selectByCode(form.getCode());
                invitationDao.selectGroupInvitations(invitation.getId());
                invitationDao.selectOidcInvitations(invitation.getId());
            }
            return templateEngine.render("my/signUp/new",
                    "signUp", form,
                    "passwordEnabled", config.isPasswordEnabled(),
                    "groupInvitations", groupInvitations,
                    "oidcInvitations", oidcInvitations);
        } else {
            User user = beansConverter.createFrom(form, User.class);
            user.setWriteProtected(false);
            UserDao userDao = daoProvider.getDao(UserDao.class);
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


            if (form.getCode() != null) {
                InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
                Invitation invitation = invitationDao.selectByCode(form.getCode());
                if (invitation == null) {
                    return templateEngine.render("my/signUp/new",
                            "signUp", form);
                }
                invitationDao.selectGroupInvitations(invitation.getId())
                        .stream()
                        .forEach(groupInvitation -> {
                            Group group = groupDao.selectById(groupInvitation.getGroupId());
                            groupDao.addUser(group, user);
                        });

                invitationDao.selectOidcInvitations(invitation.getId())
                        .stream()
                        .forEach(oidcInvitation -> userDao.connectToOidcProvider(user.getId(), oidcInvitation.getOidcProviderId(), oidcInvitation.getOidcSub()));
                invitationDao.delete(invitation);
            }

            String token = signInService.signIn(user, request);
            return signInService.responseSignedIn(token, request, "/my");
        }
    }
}
