package net.unit8.bouncr.web.controller.admin;

import enkan.collection.Parameters;
import enkan.component.BeansConverter;
import enkan.component.doma2.DomaProvider;
import enkan.data.ContentNegotiable;
import enkan.data.HttpRequest;
import enkan.data.HttpResponse;
import enkan.security.UserPrincipal;
import kotowari.component.TemplateEngine;
import kotowari.routing.UrlRewriter;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.data.JsonResponse;
import net.unit8.bouncr.util.PasswordUtils;
import net.unit8.bouncr.util.RandomUtils;
import net.unit8.bouncr.web.dao.*;
import net.unit8.bouncr.web.entity.*;
import net.unit8.bouncr.web.form.UserForm;
import net.unit8.bouncr.web.service.SignInService;
import net.unit8.bouncr.web.service.UserValidationService;
import org.seasar.doma.jdbc.SelectOptions;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static enkan.util.BeanBuilder.*;
import static enkan.util.HttpResponseUtils.RedirectStatusCode.*;

/**
 * A controller for user actions.
 *
 * @author kawasima
 */
public class UserController {
    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private DomaProvider daoProvider;

    @Inject
    private BeansConverter beansConverter;

    @Inject
    private BouncrConfiguration config;

    private UserValidationService userValidationService;

    @PostConstruct
    public void initialize() {
        userValidationService = new UserValidationService(daoProvider, config);
    }

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public List<User> list(UserPrincipal principal) {
        UserDao userDao = daoProvider.getDao(UserDao.class);

        SelectOptions options = SelectOptions.get();
        List<User> users = userDao.selectByPrincipalScope(principal, options);

        return users;
    }

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public JsonResponse show(UserPrincipal principal, Parameters params) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        boolean isLock = userDao.isLock(user.getAccount());

        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        List<UserProfile> userProfiles = userProfileFieldDao.selectValuesByUserId(user.getId());

        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        List<Group> groups = groupDao.selectByUserId(user.getId());

        return JsonResponse.fromEntity(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "account", user.getAccount(),
                "email", user.getEmail(),
                "writeProtected", user.getWriteProtected(),
                "profiles", userProfiles,
                "groups", groups,
                "isLock", isLock
        ));
    }

    @RolesAllowed({"LIST_USERS", "LIST_ANY_USERS"})
    public List<User> search(Parameters params, UserPrincipal principal) {
        String word = params.get("q");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        SelectOptions options = SelectOptions.get();
        return userDao.selectForIncrementalSearch(word, principal, options);
    }

    @RolesAllowed("CREATE_USER")
    @Transactional
    public JsonResponse create(UserForm form, HttpRequest request) {
        userValidationService.validate(form, ContentNegotiable.class.cast(request).getLocale());
        if (form.hasErrors()) {
            return JsonResponse.badRequest(form.getErrors());
        }
        User user = beansConverter.createFrom(form, User.class);
        user.setWriteProtected(false);
        UserDao userDao = daoProvider.getDao(UserDao.class);
        userDao.insert(user);

        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        List<UserProfileField> userProfileFields = userProfileFieldDao.selectAll();

        UserProfileValueDao userProfileValueDao = daoProvider.getDao(UserProfileValueDao.class);
        userProfileFields.stream()
                .forEach(userProfileField -> {
                    UserProfileValue userProfileValue = builder(new UserProfileValue())
                            .set(UserProfileValue::setUserId, user.getId())
                            .set(UserProfileValue::setUserProfileFieldId, userProfileField.getId())
                            .set(UserProfileValue::setValue, form.getProfiles().get(userProfileField.getName()))
                            .build();
                    userProfileValueDao.insert(userProfileValue);
                });

        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredentialDao.insert(builder(new PasswordCredential())
                .set(PasswordCredential::setId, user.getId())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getPassword(), salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, true)
                .build());

        GroupDao groupDao = daoProvider.getDao(GroupDao.class);
        Group bouncrUserGroup = groupDao.selectByName("BOUNCR_USER");
        groupDao.addUser(bouncrUserGroup, user);

        return JsonResponse.fromEntity(user);
    }

    @RolesAllowed({"MODIFY_USER", "MODIFY_ANY_USER"})
    public HttpResponse edit(Parameters params) {
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        UserForm form = beansConverter.createFrom(user, UserForm.class);
        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        Map<String, String> profiles = userProfileFieldDao.selectValuesByUserId(user.getId()).stream()
                .collect(Collectors.toMap(
                        UserProfile::getName,
                        UserProfile::getValue));
        form.setProfiles(profiles);

        List<UserProfileField> userProfileFields = userProfileFieldDao.selectAll();

        return templateEngine.render("admin/user/edit",
                "user", form,
                "userProfileFields", userProfileFields,
                "userId", user.getId());
    }

    @RolesAllowed({"MODIFY_USER", "MODIFY_ANY_USER"})
    @Transactional
    public HttpResponse update(UserForm form, Parameters params, HttpRequest request) {
        userValidationService.validate(form, ContentNegotiable.class.cast(request).getLocale());
        if (form.hasErrors()) {
            return templateEngine.render("admin/user/edit",
                    "user", form);
        }
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(params.getLong("id"));
        beansConverter.copy(form, user);
        userDao.update(user);

        UserProfileFieldDao userProfileFieldDao = daoProvider.getDao(UserProfileFieldDao.class);
        List<UserProfileField> userProfileFields = userProfileFieldDao.selectAll();

        UserProfileValueDao userProfileValueDao = daoProvider.getDao(UserProfileValueDao.class);
        userProfileFields.stream()
                .forEach(userProfileField -> {
                    UserProfileValue userProfileValue = builder(new UserProfileValue())
                            .set(UserProfileValue::setUserId, user.getId())
                            .set(UserProfileValue::setUserProfileFieldId, userProfileField.getId())
                            .set(UserProfileValue::setValue, form.getProfiles().get(userProfileField.getName()))
                            .build();
                    int res = userProfileValueDao.update(userProfileValue);
                    if (res == 0) {
                        userProfileValueDao.insert(userProfileValue);
                    }
                });

        PasswordCredentialDao passwordCredentialDao = daoProvider.getDao(PasswordCredentialDao.class);
        passwordCredentialDao.deleteById(user.getId());
        String salt = RandomUtils.generateRandomString(16, config.getSecureRandom());
        passwordCredentialDao.insert(builder(new PasswordCredential())
                .set(PasswordCredential::setId, user.getId())
                .set(PasswordCredential::setPassword, PasswordUtils.pbkdf2(form.getPassword(), salt, 100))
                .set(PasswordCredential::setSalt, salt)
                .set(PasswordCredential::setInitial, true)
                .build());

        return UrlRewriter.redirect(UserController.class, "list", SEE_OTHER);
    }

    @RolesAllowed({"UNLOCK_USER", "UNLOCK_ANY_USER"})
    @Transactional
    public HttpResponse unlock(Parameters params) {
        Long id = params.getLong("id");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(id);
        userDao.unlock(user.getId());
        return UrlRewriter.redirect(UserController.class, "show?id=" + id, SEE_OTHER);
    }

    @RolesAllowed({"LOCK_USER", "LOCK_ANY_USER"})
    @Transactional
    public HttpResponse lock(Parameters params) {
        Long id = params.getLong("id");
        UserDao userDao = daoProvider.getDao(UserDao.class);
        User user = userDao.selectById(id);
        userDao.lock(user.getId());
        return UrlRewriter.redirect(UserController.class, "show?id=" + id, SEE_OTHER);
    }

}
