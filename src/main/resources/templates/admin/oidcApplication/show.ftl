<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Show OpenId Connect application">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">OpenId Connect Applications</a></li>
    <li class="breadcrumb-item active">OpenId Connect Application: ${oidcApplication.name}</li>
  </ol>
  <h1>OpenId Connect application: ${oidcApplication.name}</h1>

  <h2>Scopes</h2>
  <#list oidcApplicationScopes>
  <ul>
    <#items as oidcApplicationScope>
    <li>${oidcApplicationScope.name}</li>
    </#items>
  </ul>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No scopes</p>
  </div>
  </#list>

  <div class="btn-group">
    <form method="get" action="${urlFor('edit?id=' + oidcApplication.id)}">
      <button type="submit" class="btn btn-primary">Modify</button>
    </form>
  </div>

</@layout.layout>
