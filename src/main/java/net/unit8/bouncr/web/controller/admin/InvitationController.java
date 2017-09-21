package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.GroupDao;
import net.unit8.bouncr.web.dao.InvitationDao;
import net.unit8.bouncr.web.dao.UserDao;
import net.unit8.bouncr.web.entity.Group;
import net.unit8.bouncr.web.entity.GroupInvitation;
import net.unit8.bouncr.web.entity.Invitation;
import net.unit8.bouncr.web.entity.User;
import net.unit8.bouncr.web.form.InvitationForm;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Random;

import static enkan.util.BeanBuilder.builder;

public class InvitationController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    @RolesAllowed("CREATE_INVITATION")
    public HttpResponse newForm(UserPrincipal principal) {
        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        SelectOptions options = SelectOptions.get();
        List<Group> groups = groupDao.selectByPrincipalScope(principal, options);
        return templateEngine.render("admin/invitation/new",
                "groups", groups);
    }

    @Transactional
    @RolesAllowed("CREATE_INVITATION")
    public HttpResponse create(InvitationForm form) {
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        Invitation invitation = beansConverter.createFrom(form, Invitation.class);
        invitation.setCode(code);
        InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
        invitationDao.insert(invitation);

        form.getGroupIds().forEach(groupId -> invitationDao.insertGroupInvitation(builder(new GroupInvitation())
                .set(GroupInvitation::setGroupId, groupId)
                .set(GroupInvitation::setInvitationId, invitation.getId())
                .build()));

        return templateEngine.render("admin/invitation/send");
    }

    public HttpResponse receiptForm(UserPrincipal principal, Parameters params) {
        InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
        Invitation invitation = invitationDao.selectByCode(params.get("code"));

        if (invitation != null) {
            return templateEngine.render("my/invitation/receipt",
                    "code", invitation.getCode());
        } else {
            return templateEngine.render("my/invitation/error");
        }
    }

    @Transactional
    public HttpResponse receipt(UserPrincipal principal, Parameters params) {
        InvitationDao invitationDao = daoProvider.getDao(InvitationDao.class);
        Invitation invitation = invitationDao.selectByCode(params.get("code"));

        if (invitation != null) {
            UserDao  userDao  = daoProvider.getDao(UserDao.class);
            GroupDao groupDao = daoProvider.getDao(GroupDao.class);
            invitationDao.selectGroupInvitations(invitation.getId())
                    .forEach(groupInvitation -> {
                        Group group = groupDao.selectById(groupInvitation.getGroupId());
                        User  user  = userDao.selectByAccount(principal.getName());
                        groupDao.addUser(group, user);
                    });

            invitationDao.delete(invitation);
            return templateEngine.render("admin/invitation/process");
        } else {
            return templateEngine.render("admin/invitation/error");
        }
    }
}
