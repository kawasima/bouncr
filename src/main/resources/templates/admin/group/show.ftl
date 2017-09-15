<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Show group">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Groups</a></li>
    <li class="breadcrumb-item active">Group: ${group.name}</li>
  </ol>
  <h1>Group: ${group.name}</h1>

  <h2>Users in the group</h2>
  <#list users>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.account')}</th>
        <th>${t('field.name')}</th>
      </tr>
    </thead>
    <tbody>
      <#items as user>
        <tr>
          <td>
            <a href="${urlFor('net.unit8.bouncr.web.controller.admin.UserController', 'show?id=' + user.id)}">${user.account}</a>
          </td>
          <td>${user.name}</td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No users</p>
  </div>
  </#list>

  <div class="btn-group">
    <#if !group.writeProtected>
    <form method="get" action="${urlFor('edit?id=' + group.id)}">
      <button type="submit" class="btn btn-primary">Modify</button>
    </form>
    </#if>
  </div>

</@layout.layout>
