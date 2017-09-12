package net.unit8.bouncr.web.controller;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.InvitationDao;
import net.unit8.bouncr.web.dao.PasswordCredentialDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.SignUpForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SignUpController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    public HttpResponse newForm(Parameters params) {
        String code = params.get("code");
        List<GroupInvitation> groupInvitations = Collections.emptyList();
        List<OAuth2Invitation> oauth2Invitations = Collections.emptyList();
        if (code != null) {
            InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
            Invitation invitation = invitationDao.selectByCode(code);
            invitationDao.selectGroupInvitations(invitation.getId());
            invitationDao.selectOAuth2Invitations(invitation.getId());
        }
        return templateEngine.render("my/signUp/new",
                "signUp", new SignUpForm(),
                "passwordEnabled", config.isPasswordEnabled(),
                "groupInvitations", groupInvitations,
                "oauth2Invitations", oauth2Invitations);
    }

    @Transactional
    public HttpResponse create(SignUpForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("my/signUp/new",
                    "signUp", form);
        } else {
            User user = beansConverter.createFrom(form, User.class);
            user.setWriteProtected(false);
            UserDao userDao = daoProvider.getDao(UserDao.class);
            userDao.insert(user);
            GroupDao groupDao = daoProvider.getDao(GroupDao.class);
            Group bouncrUserGroup = groupDao.selectByName("BOUNCR_USER");
            groupDao.addUser(bouncrUserGroup, user);

            if (config.isPasswordEnabled()) {
                Random random = new Random();
                PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
                passwordCredentialDao.insert(user.getId(), form.getPassword(),
                        RandomUtils.generateRandomString(random, 16));
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

                invitationDao.selectOAuth2Invitations(invitation.getId())
                        .stream()
                        .forEach(oAuth2Invitation -> {
                            userDao.connectToOAuth2Provider(user.getId(), oAuth2Invitation.getOauth2ProviderId(), oAuth2Invitation.getOauth2UserName());
                        });
                invitationDao.delete(invitation);
            }

            return templateEngine.render("my/signUp/complete");
        }
    }
}
