<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of permissions">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">Permissions</li>
  </ol>
  <h1>List of permissions</h1>

  <#list permissions>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.name')}</th>
        <th>${t('field.description')}</th>
      </tr>
    </thead>
    <tbody>
      <#items as permission>
        <tr>
          <td>
          <#if permission.writeProtected>
            ${permission.name}
          <#else>
            <a href="${urlFor('edit?id=' + permission.id)}">${permission.name}</a>
          </#if>
          </td>
          <td>${permission.description}</td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No permissions</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New permission</a>
</@layout.layout>
