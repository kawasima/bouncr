<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of OpenId connect Providers">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item active">OpenId connect providers</li>
  </ol>
  <h1>List of OpenId connect providers</h1>

  <#list oidcProviders>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.name')}</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <#items as oidcProvider>
        <tr>
          <td>
            <a href="${urlFor('edit?id='+ oidcProvider.id)}">${oidcProvider.name!''}</a>
          </td>
          <td>
          </td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>${t('info.noOidcProviders')}</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>
</@layout.layout>
