<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Sign in">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" method="post">
        <input type="text" name="account" class="form-control" placeholder="Account"/>
        <input type="password" name="password" class="form-control" placeholder="Password"/>
        <#if url??>
        <input type="hidden" name="url" value="${url}">
        </#if>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Sign in</button>

        <#list oauth2Providers>
        <hr/>

        <#items as oauth2Provider>
        <a class="btn btn-lg" href="${oauth2Provider.authorizationUrl}">Sign in by ${oauth2Provider.oauth2Provider.name}</a>
        </#items>
        </#list>
      </form>
    </div>
  </div>
</@layout.layout>

