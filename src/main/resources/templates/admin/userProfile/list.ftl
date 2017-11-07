<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of user profiles">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">User profiles</li>
  </ol>
  <h1>List of user profiles</h1>

  <#list userProfileFields>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.name')}</th>
        <th>JSON name</th>
        <th>required?</th>
        <th>identity?</th>
      </tr>
    </thead>
    <tbody>
      <#items as userProfileField>
        <tr>
          <td>
            <a href="${urlFor('edit?id=' + userProfileField.id)}">${userProfileField.name}</a>
          </td>
          <td>${userProfileField.jsonName}</td>
          <td><#if userProfileField.required><i class="fa fa-check" aria-hidden="true"></i></#if></td>
          <td><#if userProfileField.identity><i class="fa fa-check" aria-hidden="true"></i></#if></td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No user profiles</p>
  </div>
  </#list>

  <#if hasAnyPermissions(userPrincipal, "CREATE_USER_PROFILE_FIELD")>
  <a href="${urlFor('newForm')}">New register</a>
  </#if>
</@layout.layout>
