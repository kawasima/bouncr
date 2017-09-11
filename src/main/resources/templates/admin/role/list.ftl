<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of roles">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">Roles</li>
  </ol>
  <h1>List of roles</h1>

  <#list roles>
  <table class="table">
    <thead>
      <tr>
        <th>Name</th>
        <th>Description</th>
      </tr>
    </thead>
    <tbody>
      <#items as role>
        <tr>
          <td>
          <#if role.writeProtected>
            ${role.name}
          <#else>
            <a href="${urlFor('edit?id=' + role.id)}">${role.name}</a>
          </#if>
          </td>
          <td>${role.description}</td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No roles</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New role</a>
</@layout.layout>
