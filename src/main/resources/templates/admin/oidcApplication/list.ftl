<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of OpenId connect Applications">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">OpenId connect applications</li>
  </ol>
  <h1>List of OpenId connect applications</h1>

  <#list oidcApplications>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.name')}</th>
        <th>Homepage URL</th>
        <th>${t('field.description')}</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <#items as oidcApplication>
        <tr>
          <td>
            <a href="${urlFor('edit?id='+ oidcApplication.id)}">${oidcApplication.name!''}</a>
          </td>
          <td>${oidcApplication.homeUrl}</td>
          <td>${oidcApplication.description}</td>
          <td>
            <button type="button" class="btn btn-info" data-toggle="popover">Secret</button>
            <div>
              <p>Client Id: ${oidcApplication.clientId}</p>
              <p>Client Secret: ${oidcApplication.clientSecret}</p>
            </div>
          </td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>${t('info.noOidcApplications')}</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>

  <script>
<!--
    $(function() {
      $('[data-toggle="popover"]').popover();
    });
//-->
  </script>
</@layout.layout>
