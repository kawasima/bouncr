<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <div class="container">
  Hello, ${user.name}

  Your available applications
  <#list applications>
  <ul>
    <#items as application>
    <li>${application.name}</li>
    </#items>
  </ul>
  <#else>
    <div class="alert alert-info" role="alert">
       <p>No available application</p>
    </div>
  </#list>

  <h3>Administration menu for bouncer</h3>
  <ul>
    <#if hasPermission(userPrincipal, "CREATE_USER")>
    <li><a href="/admin/user">Users</a></li>
    </#if>
  </ul>

  </div>
</@layout.layout>
