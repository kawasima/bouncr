<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <h1>Administration</h1>

  <ul class="list-group">
    <#if hasPermission(userPrincipal, "LIST_USERS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.UserController', 'list')}">Users</a>
    </li>
    </#if>

    <#if hasPermission(userPrincipal, "LIST_GROUPS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.GroupController', 'list')}">Groups</a>
    </li>
    </#if>

    <#if hasPermission(userPrincipal, "LIST_APPLICATIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.ApplicationController', 'list')}">Applications</a></li>
    </#if>

    <#if hasPermission(userPrincipal, "LIST_ROLES")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.RoleController', 'list')}">Roles</a>
    </li>
    </#if>

    <#if hasPermission(userPrincipal, "LIST_PERMISSIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.PermissionController', 'list')}">Permissions</a>
    </li>
    </#if>

    <#if hasPermission(userPrincipal, "LIST_OAUTH2_APPLICATIONS")>
    <li class="list-group-item">
      <a href="${urlFor('net.unit8.bouncr.web.controller.admin.OAuth2ApplicationController', 'list')}"><i class="fa fa-ticket" aria-hidden="true"></i>OAuth2 Applications</a>
    </li>
    </#if>
  </ul>
</@layout.layout>
