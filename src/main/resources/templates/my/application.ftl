<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "My applications">
  <h1>Hello, ${user.name}</h1>

  <h2>Your available applications</h2>

  <#list applications>
  <ul>
    <#items as application>
    <li class="card text-center">
      <div class="card-body">
        <h4 class="card-title">${application.name}</h4>
        <p class="card-text">${application.description}</p>
        <a href="${application.topPage}" class="btn btn-primary">Go ${application.name}</a>
      </div>
    </li>
    </#items>
  </ul>

  <#else>
  <div class="alert alert-info" role="alert">
    <p>No available application</p>
  </div>
  </#list>
</@layout.layout>
