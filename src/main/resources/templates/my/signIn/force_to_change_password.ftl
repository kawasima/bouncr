<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Change your password">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" action="${urlFor('forceToChangePassword')}" method="post">
        <input type="hidden" name="account" value="${changePassword.account}" class="form-control"/>
        <input type="hidden" name="oldPassword" value="${changePassword.oldPassword}" class="form-control"/>

        <input type="password" name="newPassword" class="form-control" placeholder="${t('field.password')}"/>
        <#if changePassword.hasErrors('newPassword')>
        <div class="alert alert-danger" role="alert">
        <#list changePassword.getErrors('newPassword') as err>
        ${err}
        </#list>
        </div>
        </#if>

        <#if url??>
        <input type="hidden" name="url" value="${changePassword.url}">
        </#if>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Change</button>
      </form>
    </div>
  </div>
</@layout.layout>
