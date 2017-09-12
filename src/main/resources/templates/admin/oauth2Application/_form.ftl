<div class="form-group<#if oauth2Application.hasErrors("name")> has-error</#if>">
  <label for="name">Name</label>
  <input id="name" class="form-control<#if oauth2Application.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${oauth2Application.name!''}"/>
  <span class="invalid-feedback"><#if oauth2Application.hasErrors("name")>${oauth2Application.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if oauth2Application.hasErrors("description")> has-error</#if>">
  <label for="description">Description</label>
  <input id="description" class="form-control<#if oauth2Application.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${oauth2Application.description!''}"/>
  <span class="invalid-feedback"><#if oauth2Application.hasErrors("description")>${oauth2Application.getErrors("description")?join(",")}</#if></span>
</div>

<div class="form-group<#if oauth2Application.hasErrors("homeUrl")> has-error</#if>">
  <label for="homeUrl">Homepage URL</label>
  <input id="homeUrl" class="form-control<#if oauth2Application.hasErrors("homeUrl")> is-invalid</#if>" type="text" name="homeUrl" value="${oauth2Application.homeUrl!''}"/>
  <span class="invalid-feedback"><#if oauth2Application.hasErrors("homeUrl")>${oauth2Application.getErrors("homeUrl")?join(",")}</#if></span>
</div>

<div class="form-group<#if oauth2Application.hasErrors("callbackUrl")> has-error</#if>">
  <label for="callbackUrl">User authorization callback URL</label>
  <input id="callbackUrl" class="form-control<#if oauth2Application.hasErrors("callbackUrl")> is-invalid</#if>" type="text" name="callbackUrl" value="${oauth2Application.callbackUrl!''}"/>
  <span class="invalid-feedback"><#if oauth2Application.hasErrors("callbackUrl")>${oauth2Application.getErrors("callbackUrl")?join(",")}</#if></span>
</div>

