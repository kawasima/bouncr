<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of groups">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">Groups</li>
  </ol>
  <h1>List of groups</h1>

  <#list groups>
  <table class="table">
    <thead>
      <tr>
        <th>Name</th>
        <th>Description</th>
      </tr>
    </thead>
    <tbody>
      <#items as group>
        <tr>
          <td>
            <#if group.writeProtected>
            ${group.name}
            <#else>
            <a href="${urlFor('edit?id='+ group.id)}">${group.name}</a>
            </#if>
          </td>
          <td>${group.description}</td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No groups</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>
</@layout.layout>
