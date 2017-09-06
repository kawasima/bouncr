<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <div class="jumbotron">
    <div class="container">
      <h1>Hello, ${user.name}</p>

      <h2>Your available applications</h2>
      <#list applications>
      <ul>
        <#items as application>
        <li><a href="${application.topPage}">${application.name}</a></li>
        </#items>
      </ul>
      <#else>
        <div class="alert alert-info" role="alert">
          <p>No available application</p>
        </div>
      </#list>
    </div>
  </div>

  <h2>Sign in</h2>
  <#list signInHistories>
  <table class="table">
    <thead>
      <tr>
        <th>Account</th>
        <th>Date</th>
        <th>Successful?</th>
      </tr>
    </thead>
    <tbody>
      <#items as hist>
      <tr class="table-<#if hist.successful>success<#else>danger</#if>">
        <td>${hist.account}</td>
        <td>${hist.signedInAt}</td>
        <td>
          <#if hist.successful>
          <i class="fa fa-check" aria-hidden="true"></i>
          <#else>
          <i class="fa fa-ban" aria-hidden="true"></i>
          </#if>
        </td>
      </tr>
      </#items>
    </tbody>
  </table>
  </#list>
</@layout.layout>
