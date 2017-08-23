<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of permissions">
  <h1>List of permissions</h1>

  <#list permissions>
  <table class="table">
    <thead>
      <tr>
        <th>Name</th>
        <th>Description</th>
      </tr>
    </thead>
    <tbody>
      <#items as permission>
        <tr>
          <td>
          <#if role.writeProtected>
            ${role.name}
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
