<div class="form-group<#if signUp.hasErrors("account")> has-error</#if>">
  <label for="account">Account</label>
  <input id="account" name="account" class="form-control<#if signUp.hasErrors("name")> is-invalid</#if>" value="${signUp.account!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("account")>${signUp.getErrors("account")?join(",")}</#if></span>
</div>

<div class="form-group<#if signUp.hasErrors("name")> has-error</#if>">
  <label for="name">Name</label>
  <input id="name" name="name" class="form-control<#if signUp.hasErrors("name")> is-invalid</#if>" value="${signUp.name!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("name")>${singUp.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if signUp.hasErrors("email")> has-error</#if>">
  <label for="email">Email</label>
  <input id="email" name="email" class="form-control<#if signUp.hasErrors("email")> is-invalid</#if>" value="${signUp.email!''}"/>
  <span class="invalid-feedback"><#if signUp.hasErrors("email")>${signUp.getErrors("email")?join(",")}</#if></span>
</div>

<#if passwordEnabled>
<div class="form-group<#if signUp.hasErrors("password")> has-error</#if>">
  <label for="password">Password</label>
  <input id="password" class="form-control<#if signUp.hasErrors("password")> is-invalid</#if>" type="password" name="password" value=""/>
  <span class="invalid-feedback"><#if signUp.hasErrors("password")>${signUp.getErrors("password")?join(",")}</#if></span>
</div>
</#if>

<#list oauth2Invitations>
<div class="form-group">
  <#items as oauth2Invitation>
  <button class="btn" type="button">${oauth2Invitation.oauth2ProviderId}</button>
  </#items>
</div>
</#list>
