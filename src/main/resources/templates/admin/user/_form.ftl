<div class="form-group<#if user.hasErrors("account")> has-error</#if>">
  <label for="account">${t('field.account')}</label>
  <#if editMode>
  ${user.account!''}
  <input name="account" type="hidden" value="${user.account}"/>
  <#else>
  <input id="account" name="account" class="form-control<#if user.hasErrors("name")> is-invalid</#if>" value="${user.account!''}"/>
  </#if>
  <span class="invalid-feedback"><#if user.hasErrors("account")>${user.getErrors("account")?join(",")}</#if></span>
</div>

<div class="form-group<#if user.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" name="name" class="form-control<#if user.hasErrors("name")> is-invalid</#if>" value="${user.name!''}"/>
  <span class="invalid-feedback"><#if user.hasErrors("name")>${user.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if user.hasErrors("email")> has-error</#if>">
  <label for="email">${t('field.email')}</label>
  <input id="email" name="email" class="form-control<#if user.hasErrors("email")> is-invalid</#if>" value="${user.email!''}"/>
  <span class="invalid-feedback"><#if user.hasErrors("email")>${user.getErrors("email")?join(",")}</#if></span>
</div>

<div class="form-group<#if user.hasErrors("password")> has-error</#if>">
  <label for="password">${t('field.password')}</label>
  <input id="password" class="form-control<#if user.hasErrors("password")> is-invalid</#if>" type="password" name="password" value=""/>
  <span class="invalid-feedback"><#if user.hasErrors("password")>${user.getErrors("password")?join(",")}</#if></span>
</div>
