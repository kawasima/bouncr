<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "Home">
  <div class="jumbotron mt-3">
    <div class="container">
      <h1>Hello, ${user.name}</h1>

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
    <#assign userAgent = parseUserAgent(userSession.userAgent)>
    <li class="list-group-item">
      <div class="row">
        <div class="col-md-2">
        <#switch userAgent["category"]>
          <#case "pc">
            <i class="fa fa-desktop fa-4x" aria-hidden="true"></i>
            <#break>
          <#case "smartphone">
            <i class="fa fa-mobile fa-4x" aria-hidden="true"></i>
            <#break>
        </#switch>
        </div>
        <div class="col-md-10">
          <h4>${userSession.remoteAddress!''}</h4>
          <#if token == userSession.token>
          <p>${t('info.yourCurrentSession')}</p>
          </#if>
          <p>${userAgent["name"]!''} on ${userAgent["os"]!''} ${userAgent["os_version"]!''}</p>
          <p>Signed in: ${userSession.createdAt}</p>
          <#if token != userSession.token>
          <form action="${urlFor('revokeSession?id=' + userSession.id)}" method="post">
            <button class="btn" type="submit">Revoke</button>
          </form>
          </#if>
        </div>
      </div>
    </li>
    </#items>
  </ul>
  <#else>
  <div class="alert alert-info" role="alert">>
    <p>No sessions</p>
  </div>
  </#list>
</@layout.layout>
