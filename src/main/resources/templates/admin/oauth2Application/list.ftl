<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of OAuth2 Applications">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">OAuth2 applications</li>
  </ol>
  <h1>List of oauth2 applications</h1>

  <#list oauth2Applications>
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
      <#items as oauth2Application>
        <tr>
          <td>
            <a href="${urlFor('edit?id='+ oauth2Application.id)}">${oauth2Application.name!''}</a>
          </td>
          <td>${oauth2Application.homeUrl}</td>
          <td>${oauth2Application.description}</td>
          <td>
            <button type="button" class="btn btn-info" data-toggle="popover">Secret</button>
            <div>
              <p>Client Id: ${oauth2Application.clientId}</p>
              <p>Client Secret: ${oauth2Application.clientSecret}</p>
            </div>
          </td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>${t('info.noOAuth2Applications')}</p>
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
