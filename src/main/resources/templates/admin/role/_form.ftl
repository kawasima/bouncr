<div class="form-group<#if role.hasErrors("name")> has-danger</#if>">
    <label for="name">${t('field.name')}</label>
    <input id="name" class="form-control<#if role.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${role.name!''}"/>
    <span class="invalid-feedback"><#if role.hasErrors("name")>${role.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if role.hasErrors("description")> has-danger</#if>">
    <label for="description">${t('field.description')}</label>
    <input id="description" class="form-control<#if role.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${role.description!''}"/>
    <span class="invalid-feedback"><#if role.hasErrors("description")>${role.getErrors("description")?join(",")}</#if></span>
</div>

<select name="permissionId[]" class="selectpicker" multiple>
  <#list permissions as permission>
  <option value="${permission.id}"<#if rolePermissionIds?seq_contains(permission.id)> selected</#if>>${permission.name}</option>
  </#list>
</select>
