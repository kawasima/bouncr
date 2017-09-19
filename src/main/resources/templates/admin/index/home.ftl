<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <h1>Administration</h1>

  <ul class="list-group">
    <#if hasAnyPermissions(userPrincipal, "LIST_USERS", "LIST_ANY_USERS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.UserController', 'list')}">Users</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_GROUPS", "LIST_ANY_GROUPS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.GroupController', 'list')}">Groups</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_APPLICATIONS", "LIST_ANY_APPLICATIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.ApplicationController', 'list')}">Applications</a></li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_ROLES", "LIST_ANY_ROLES")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.RoleController', 'list')}">Roles</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_PERMISSIONS", "LIST_ANY_PERMISSIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.PermissionController', 'list')}">Permissions</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_OIDC_APPLICATIONS", "LIST_ANY_OIDC_APPLICATIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.OidcApplicationController', 'list')}"><i class="fa fa-ticket" aria-hidden="true"></i>OpenId connect Applications</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "LIST_OAUTH2_PROVIDERS", "LIST_ANY_OAUTH2_PROVIDERS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.OAuth2ProviderController', 'list')}"><i class="fa fa-ticket" aria-hidden="true"></i>OAuth2 Providers</a>
    </li>
    </#if>

    <#if hasAnyPermissions(userPrincipal, "CREATE_INVITATION")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.InvitationController', 'list')}">Invitation</a>
    </li>
    </#if>
  </ul>
</@layout.layout>
