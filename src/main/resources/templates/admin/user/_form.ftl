<div class="form-group<#if user.hasErrors("account")> has-error</#if>">
  <label for="account">Account</label>
  <input id="account" class="form-control" value="${user.name!''}"/>
  <span class="help-block"><#if user.hasErrors("account")>${user.getErrors("account")?join(",")}</#if></span>
</div>

<div class="form-group<#if user.hasErrors("email")> has-error</#if>">
  <label for="email">Account</label>
  <input id="email" class="form-control" value="${user.email!''}"/>
  <span class="help-block"><#if user.hasErrors("email")>${user.getErrors("email")?join(",")}</#if></span>
</div>

<div class="form-group<#if user.hasErrors("password")> has-error</#if>">
  <label for="password">Password</label>
  <input id="password" class="form-control" type="password" name="password" value=""/>
  <span class="help-block"><#if user.hasErrors("password")>${user.getErrors("password")?join(",")}</#if></span>
</div>
