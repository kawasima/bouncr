<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of users">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" action="${urlFor('signInByPassword')}" method="post">
        <input type="number" name="code" class="form-control" placeholder="Code"/>
        <input type="hidden" name="account" value="${signin.account}"/>
        <input type="hidden" name="password" value="${signin.password}"/>
        <#if url??>
        <input type="hidden" name="url" value="${signin.url}">
        </#if>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Sign in</button>
      </form>
    </div>
  </div>
</@layout.layout>
