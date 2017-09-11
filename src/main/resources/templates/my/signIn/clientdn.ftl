<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of users">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" action="${urlFor('loginByClientDN')}" method="post">
        <input type="text" name="account" value="${signin.account}" class="form-control" placeholder="Account" readonly="readonly"/>
        <#if url??>
        <input type="hidden" name="url" value="${signin.url}">
        </#if>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Sign in</button>
      </form>
    </div>
  </div>
</@layout.layout>
