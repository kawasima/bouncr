package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.HttpResponse;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.data.JsonResponse;
import net.unit8.bouncr.web.dao.ApplicationDao;
import net.unit8.bouncr.web.entity.Application;
import net.unit8.bouncr.web.form.ApplicationForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static enkan.util.HttpResponseUtils.RedirectStatusCode.SEE_OTHER;

/**
 * A controller about application actions.
 *
 * @author kawasima
 */
public class ApplicationController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private DomaProvider daoProvider;

    @RolesAllowed({"LIST_APPLICATIONS", "LIST_ANY_APPLICATIONS"})
    public JsonResponse list() {
        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        List<Application> applications = applicationDao.selectAll();
        return JsonResponse.fromEntity(applications);
    }

    @RolesAllowed("CREATE_APPLICATION")
    @Transactional
    public HttpResponse create(ApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/application/new",
                    "application", form);
        } else {
            Application application = beansConverter.createFrom(form, Application.class);
            application.setWriteProtected(false);
            application.setId(null);
            if (application.getVirtualPath().endsWith("/")) {
                application.setVirtualPath(application.getVirtualPath().replaceFirst("[\\s/]*$", ""));
            }

            ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
            applicationDao.insert(application);

            return UrlRewriter.redirect(ApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed({"MODIFY_APPLICATION", "MODIFY_ANY_APPLICATION"})
    @Transactional
    public HttpResponse update(ApplicationForm form) {
        if (form.hasErrors()) {
            return templateEngine.render("admin/application/edit",
                    "application", form);
        } else {
            ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
            Application application = applicationDao.selectById(form.getId());
            beansConverter.copy(form, application);
            applicationDao.update(application);

            return UrlRewriter.redirect(ApplicationController.class, "list", SEE_OTHER);
        }
    }

    @RolesAllowed({"DELETE_APPLICATION", "DELETE_ANY_APPLICATION"})
    @Transactional
    public HttpResponse delete(Parameters params) {
        ApplicationDao applicationDao = daoProvider.getDao(ApplicationDao.class);
        Application application = applicationDao.selectById(params.getLong("id"));
        applicationDao.delete(application);

        return UrlRewriter.redirect(ApplicationController.class, "list", SEE_OTHER);
    }
}
