<input type="hidden" name="code" value="${signUp.code!''}"/>

<div class="form-group<#if signUp.hasErrors("account")> has-error</#if>">
  <label for="account">${t('field.account')}</label>
  <input id="account" name="account" class="form-control<#if signUp.hasErrors("account")> is-invalid</#if>" value="${signUp.account!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("account")>${signUp.getErrors("account")?join(",")}</#if></span>
</div>

<div class="form-group<#if signUp.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" name="name" class="form-control<#if signUp.hasErrors("name")> is-invalid</#if>" value="${signUp.name!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("name")>${signUp.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if signUp.hasErrors("email")> has-error</#if>">
  <label for="email">${t('field.email')}</label>
  <input id="email" name="email" class="form-control<#if signUp.hasErrors("email")> is-invalid</#if>" value="${signUp.email!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("email")>${signUp.getErrors("email")?join(",")}</#if></span>
</div>

<#if passwordEnabled>
<div class="form-group<#if signUp.hasErrors("password")> has-error</#if>">
  <label for="password">${t('field.password')}</label>
  <div class="form-row">
    <div class="col">
      <input id="password" class="form-control<#if signUp.hasErrors("validPasswordWhenEnabled")> is-invalid</#if>" type="password" name="password" value=""/>
  <span class="invalid-feedback"><#if signUp.hasErrors("validPasswordWhenEnabled")>${signUp.getErrors("validPasswordWhenEnabled")?join(",")}</#if></span>
    </div>
    <div class="col">
      <div class="form-check">
        <label class="form-check-label">
          <input id="password-disabled" class="form-check-input" name="passwordDisabled" type="checkbox" value="true">
          ${t('label.disablePassword')}
        </label>
      </div>
    </div>
  </div>
</div>
</#if>

<#list groupInvitations>
<div class="form-group">
    <ul>
      <#items as groupInvitation>
        <li>${groupInvitation.groupId}</li>
      </#items>
    </ul>
</div>
</#list>

<script>
document
  .querySelector("#password-disabled")
  .addEventListener("change", function(e) {
    document.querySelector("#password").disabled = "disabled";
  });
</script>
