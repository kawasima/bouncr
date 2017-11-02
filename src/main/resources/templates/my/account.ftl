<#import "../layout/defaultLayout.ftl" as layout>
<@layout.layout "Account">
  <h1>Account</h1>

  <h2>Change password</h2>
  <#if message??>
  <div class="alert alert-danger" role="alert">${t(message)}</div>
  </#if>
  <form action="${urlFor('changePassword')}" method="post" class="mb-5">
    <div class="form-group<#if changePassword.hasErrors("oldPassword")> has-error</#if>">
      <label for="old-password">${t('field.oldPassword')}</label>
      <input id="old-password" class="form-control<#if changePassword.hasErrors("oldPassword")> is-invalid</#if>" type="password" name="oldPassword" value=""/>
      <span class="invalid-feedback"><#if changePassword.hasErrors("oldPassword")>${changePassword.getErrors("oldPassword")?join(",")}</#if></span>
    </div>

    <div class="form-group<#if changePassword.hasErrors("newPassword")> has-error</#if>">
      <label for="new-password">${t('field.newPassword')}</label>
      <input id="new-password" class="form-control<#if changePassword.hasErrors("newPassword")> is-invalid</#if>" type="password" name="newPassword" value=""/>
      <span class="invalid-feedback"><#if changePassword.hasErrors("newPassword")>${changePassword.getErrors("newPassword")?join(",")}</#if></span>
    </div>

    <button type="submit" class="btn btn-primary">Update password</button>
  </form>

  <h2>Two-factor authentication</h2>

  <form action="${urlFor('switchTwoFactorAuth?enabled=' + (!(twofaSecret??))?string("on","off"))}" method="post">
    <#if twofaSecret??>
    <button class="btn btn-success" type="submit">ON</button>
    <#else>
    <button class="btn" type="submit">OFF</button>
    </#if>
  </form>

  <#if twofaSecret??>
  <canvas id="qrcode"></canvas>
  <script src="/my/assets/js/qrcode.min.js"></script>
  <script type="text/javascript">
    qrcodelib.toCanvas(document.getElementById('qrcode'),
    "otpauth://totp/Bouncr:${user.account}?secret=${twofaSecret}&issure=Bouncr",
    function (error) {
      if (error) console.error(error)
      console.log('success!');
    })
  </script>
  </#if>
</@layout.layout>
