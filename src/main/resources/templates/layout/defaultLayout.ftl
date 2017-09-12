<#macro layout title="Layout example">
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <link rel="shortcut icon" type="image/x-icon" href="/my/assets/img/favicon.ico">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title} | Bouncr</title>
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css" integrity="sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M" crossorigin="anonymous">
  <link href="https://cdn.jsdelivr.net/fontawesome/4.7.0/css/font-awesome.min.css" rel="stylesheet"/>
  <link href="/my/assets/css/bouncr.css" rel="stylesheet"/>
  <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js" integrity="sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN" crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.11.0/umd/popper.min.js" integrity="sha384-b/U6ypiBEHpOf/4+1nzFpr53nxSS+GLCkfwBdFNTxtclqqenISfwAzpKaMNFNmj4" crossorigin="anonymous"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/js/bootstrap.min.js" integrity="sha384-h0AbiXch4ZDo7tp9hKZ4TsHbi047NrKGLO3SEJAg45jXxnGIfYzk4Si90RDIqNm1" crossorigin="anonymous"></script>
  <link rel="stylesheet" href="https://cdn.rawgit.com/infostreams/bootstrap-select/fd227d46de2afed300d97fd0962de80fa71afb3b/dist/css/bootstrap-select.min.css" />
  <script src="https://cdn.rawgit.com/infostreams/bootstrap-select/fd227d46de2afed300d97fd0962de80fa71afb3b/dist/js/bootstrap-select.min.js"></script>
</head>
<body>
  <nav class="navbar navbar-dark bg-dark navba-expand-md">
    <a class="navbar-brand" href="/my">
      <img src="/my/assets/img/logo.svg" width="200" class="d-inline-block align-top" alt="bouncr">
    </a>
    <#if userPrincipal??>
    <div class="btn-group">
      <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        You
      </button>
      <div class="dropdown-menu dropdown-menu-right" aria-labelledby="dropdownMenuButton">
        <#if hasAnyPermissions(userPrincipal, "LIST_USERS", "LIST_GROUPS")>
          <a class="dropdown-item" href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Admin</a>
        </#if>
        <a class="dropdown-item" href="${urlFor('net.unit8.bouncr.web.controller.MyController', 'account')}">${t('label.account')}</a>
        <form name="signout" method="post" action="/my/signOut">
          <button class="dropdown-item" type="submit">${t('label.signout')}</button>
        </form>
      </div>
    </div>
    </#if>
  </nav>
  <div class="container">
    <#nested/>
  </div>
</body>
</html>
</#macro>
