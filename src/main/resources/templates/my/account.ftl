<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "Change password">
  <h1>Change password</h1>

  <#if message??>
  <div class="alert alert-danger" role="alert">${t(message)}</div>
  </#if>
  <form action="${urlFor('changePassword')}" method="post">
    <div class="form-group<#if user.hasErrors("oldPassword")> has-error</#if>">
      <label for="old-password">${t('field.oldPassword')}</label>
      <input id="old-password" class="form-control<#if user.hasErrors("oldPassword")> is-invalid</#if>" type="password" name="oldPassword" value=""/>
      <span class="invalid-feedback"><#if user.hasErrors("oldPassword")>${user.getErrors("oldPassword")?join(",")}</#if></span>
    </div>

    <div class="form-group<#if user.hasErrors("newPassword")> has-error</#if>">
      <label for="new-password">${t('field.newPassword')}</label>
      <input id="new-password" class="form-control<#if user.hasErrors("newPassword")> is-invalid</#if>" type="password" name="newPassword" value=""/>
      <span class="invalid-feedback"><#if user.hasErrors("newPassword")>${user.getErrors("newPassword")?join(",")}</#if></span>
    </div>

    <button type="submit" class="btn btn-primary">Update password</button>
  </form>

</@layout.layout>
