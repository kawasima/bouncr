<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Show user">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Users</a></li>
    <li class="breadcrumb-item active">${user.account}</li>
  </ol>
  <h1>User: ${user.account}</h1>

  <#list groups>
  <h2>Groups</h2>
  <ul>
    <#items as group>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.admin.GroupController', 'edit?id=' + group.id)}">${group.name}</a></li>
    </#items>
  </ul>
  </#list>

  <div class="btn-group">
    <#if hasAnyPermissions(userPrincipal, "MODIFY_USER", "MODIFY_ANY_USER")>
    <a class="btn" href="${urlFor('edit?id=' + user.id)}">Modify</a>
    </#if>

    <#if isLock>
    <form class="form-inline" method="post" action="${urlFor('unlock?id=' + user.id)}">
      <button class="btn" type="submit">${t('label.unlock')}</button>
    </form>
    <#else>
    <form class="form-inline" method="post" action="${urlFor('lock?id=' + user.id)}">
      <button class="btn" type="submit">${t('label.lock')}</button>
    </form>
    </#if>
  </div>

</@layout.layout>
