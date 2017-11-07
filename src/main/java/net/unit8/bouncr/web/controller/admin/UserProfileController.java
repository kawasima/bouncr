package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.web.dao.UserProfileFieldDao;
import net.unit8.bouncr.web.entity.UserProfileField;
import net.unit8.bouncr.web.form.UserProfileForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

public class UserProfileController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    @RolesAllowed("LIST_USER_PROFILE_FIELDS")
    public HttpResponse list(UserPrincipal principal) {
        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        List<UserProfileField> userProfileFields = userProfileFieldDao.selectAll();
        return templateEngine.render("admin/userProfile/list",
                "userProfileFields", userProfileFields);
    }

    @RolesAllowed("CREATE_USER_PROFILE_FIELD")
    public HttpResponse newForm() {
        UserProfileForm form = new UserProfileForm();
        return templateEngine.render("admin/userProfile/new",
                "userProfileField", form);
    }

    @RolesAllowed("MODIFY_USER_PROFILE_FIELD")
    public HttpResponse edit(Parameters params) {
        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        UserProfileField userProfileField = userProfileFieldDao.selectById(params.getLong("id"));
        UserProfileForm form = beansConverter.createFrom(userProfileField, UserProfileForm.class);
        return templateEngine.render("admin/userProfile/edit",
                "userProfileField", form);
    }

    @RolesAllowed("CREATE_USER_PROFILE_FIELD")
    @Transactional
    public HttpResponse create(UserProfileForm form) {
        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        UserProfileField userProfileField = beansConverter.createFrom(form, UserProfileField.class);
        int position = userProfileFieldDao.selectAll().stream()
                .max(Comparator.comparingInt(UserProfileField::getPosition))
                .map(UserProfileField::getPosition)
                .orElse(0);

        userProfileField.setPosition(position + 1);
        userProfileFieldDao.insert(userProfileField);
        return UrlRewriter.redirect(UserProfileController.class, "list", SEE_OTHER);
    }

    @RolesAllowed("MODIFY_USER_PROFILE_FIELD")
    @Transactional
    public HttpResponse update(Parameters params, UserProfileForm form) {
        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        UserProfileField userProfileField = userProfileFieldDao.selectById(params.getLong("id"));
        beansConverter.copy(form, userProfileField);
        userProfileFieldDao.update(userProfileField);
        return UrlRewriter.redirect(UserProfileController.class, "list", SEE_OTHER);
    }

}
