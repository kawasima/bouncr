<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Sign in">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" method="post">
        <input type="text" name="account" class="form-control" placeholder="${t('field.account')}" value="${signin.account!''}"/>
        <input type="password" name="password" class="form-control" placeholder="${t('field.password')}"/>
        <#if signin.hasErrors('account')>
        <div class="alert alert-danger" role="alert">
          <#list signin.getErrors('account') as err>
          ${t(err)}
          </#list>
        </div>
        </#if>
        <#if signin.url??>
        <input type="hidden" name="url" value="${signin.url}">
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
    <a href="${urlFor('net.unit8.bouncr.web.controller.SignUpController', 'newForm')}" class="text-center new-account">Create an account</a>
  </div>
</@layout.layout>

