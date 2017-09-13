<div class="form-group<#if permission.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" class="form-control<#if permission.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${permission.name!''}"/>
  <span class="invalid-feedback"><#if permission.hasErrors("name")>${permission.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if permission.hasErrors("description")> has-error</#if>">
  <label for="description">${t('field.description')}</label>
  <input id="description" class="form-control<#if permission.hasErrors("description")> is-invalid</#if>" type="text" name="name" value="${permission.description!''}"/>
  <span class="invalid-feedback"><#if permission.hasErrors("description")>${permission.getErrors("description")?join(",")}</#if></span>
</div>
