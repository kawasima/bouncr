<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Show user">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Users</a></li>
    <li class="breadcrumb-item active">${user.account}</li>
  </ol>
  <h1>User: ${user.account}
    <#if isLock> <i class="fa fa-lock" aria-hidden="true"></i></#if>
  </h1>

  <div class="col-xs-12 col-sm-6 col-md-6 mb-5">
    <div class="well well-sm">
      <div class="row">
        <div class="col-sm-6 col-md-4">
          <img src="https://www.gravatar.com/avatar/${md5hex(user.email)}?s=160"/>
        </div>
        <div class="col-sm-6 col-md-8">
          <p>
            <i class="fa fa-id-card-o" aria-hidden="true"></i>${user.name}
            <br/>
            <i class="fa fa-envelope-o" aria-hidden="true"></i>${user.email}
          </p>
          <#list userProfiles>
          <p>
            <#items as userProfile>
            ${userProfile.name}: ${userProfile.value}
            <br/>
            </#items>
          </p>
          </#list>
        </div>
      </div>
    </div>
  </div>

  <#list groups>
  <h2>Groups</h2>
  <ul>
    <#items as group>
    <li><a href="${urlFor('net.unit8.bouncr.web.controller.admin.GroupController', 'edit?id=' + group.id)}">${group.name}</a></li>
    </#items>
  </ul>
  </#list>

  <#if !user.writeProtected>
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
  </#if>

</@layout.layout>
