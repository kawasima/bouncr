<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <h1>Home</h1>

  <ul>
    <#if hasAnyPermission(userPrincipal, "CREATE_USER", "MODIFY_USER", "DELETE_USER")>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.UserController', 'list')}"><i class="fa fa-user" aria-hidden="true"></i></span>Users</a></li>
    </#if>
    <#if hasAnyPermission(userPrincipal, "CREATE_GROUP", "MODIFY_GROUP", "DELETE_GROUP")>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.GroupController', 'list')}"><i class="fa fa-users" aria-hidden="true"></i>Groups</a></li>
    </#if>
    <#if hasAnyPermission(userPrincipal, "CREATE_APPLICATION", "MODIFY_APPLICATION", "DELETE_APPLICATION")>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.ApplicationController', 'list')}"><i class="fa fa-cube" aria-hidden="true"></i>Applications</a></li>
    </#if>
    <#if hasAnyPermission(userPrincipal, "CREATE_ROLE", "MODIFY_ROLE", "DELETE_ROLE")>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.RoleController', 'list')}"><i class="fa fa-id-badge" aria-hidden="true"></i>Roles</a></li>
    </#if>
    <#if hasAnyPermission(userPrincipal, "CREATE_PERMISSION", "MODIFY_PERMISSION", "DELETE_PERMISSION")>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.PermissionController', 'list')}"><i class="fa fa-ticket" aria-hidden="true"></i>Permissions</a></li>
    </#if>
  </ul>
</@layout.layout>
