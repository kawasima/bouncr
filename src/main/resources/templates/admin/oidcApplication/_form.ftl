<div class="form-group<#if oidcApplication.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" class="form-control<#if oidcApplication.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${oidcApplication.name!''}"/>
  <span class="invalid-feedback"><#if oidcApplication.hasErrors("name")>${oidcApplication.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcApplication.hasErrors("description")> has-error</#if>">
  <label for="description">${t('field.description')}</label>
  <input id="description" class="form-control<#if oidcApplication.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${oidcApplication.description!''}"/>
  <span class="invalid-feedback"><#if oidcApplication.hasErrors("description")>${oidcApplication.getErrors("description")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcApplication.hasErrors("homeUrl")> has-error</#if>">
  <label for="homeUrl">Homepage URL</label>
  <input id="homeUrl" class="form-control<#if oidcApplication.hasErrors("homeUrl")> is-invalid</#if>" type="text" name="homeUrl" value="${oidcApplication.homeUrl!''}"/>
  <span class="invalid-feedback"><#if oidcApplication.hasErrors("homeUrl")>${oidcApplication.getErrors("homeUrl")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcApplication.hasErrors("callbackUrl")> has-error</#if>">
  <label for="callbackUrl">User authorization callback URL</label>
  <input id="callbackUrl" class="form-control<#if oidcApplication.hasErrors("callbackUrl")> is-invalid</#if>" type="text" name="callbackUrl" value="${oidcApplication.callbackUrl!''}"/>
  <span class="invalid-feedback"><#if oidcApplication.hasErrors("callbackUrl")>${oidcApplication.getErrors("callbackUrl")?join(",")}</#if></span>
</div>

<div class="form-group">
  <label for="permissionId">Scopes</label>
  <select class="form-control" name="permissionId[]" multiple>
    <#list permissions as permission>
    <option value="${permission.id}"<#if oidcApplication.permissionId?has_content && oidcApplication.permissionId?seq_contains(permission.id)> selected</#if>>${permission.name}</option>
    </#list>
  </select>
</div>


