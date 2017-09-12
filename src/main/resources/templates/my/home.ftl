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

  <#list userActions>
  <table class="table">
    <thead>
      <tr>
        <th>Type</th>
        <th>Date</th>
        <th>Actor IP</th>
      </tr>
    </thead>
    <tbody>
      <#items as userAction>
      <tr>
        <td>${userAction.actionType.getName()}</td>
        <td>${userAction.createdAt}</td>
        <td>${userAction.actorIp}</td>
      </tr>
      </#items>
    </tbody>
  </table>
  </#list>

  <h2>Sessions</h2>
  <#list userSessions>
  <p>This is a list of devices that have signed in your account. Revoke any sessions that you do not recognize.</p>
  <ul class="list-group">
    <#items as userSession>
    <li class="list-group-item">
      <#if userSession.userAgent??>
      <p>${userSession.userAgent}</p>
      </#if>
      <p>Signed in: ${userSession.createdAt}</p>
    </li>
    </#items>
  </ul>
  <#else>
  <div class="alert alert-info" role="alert">>
    <p>No sessions</p>
  </div>
  </#list>
</@layout.layout>
