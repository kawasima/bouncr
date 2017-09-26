<div class="form-group<#if group.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" class="form-control<#if group.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${group.name!''}"/>
  <span class="invalid-feedback"><#if group.hasErrors("name")>${group.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if group.hasErrors("description")> has-error</#if>">
  <label for="description">${t('field.description')}</label>
  <input id="description" class="form-control<#if group.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${group.description!''}"/>
  <span class="invalid-feedback"><#if group.hasErrors("description")>${group.getErrors("description")?join(",")}</#if></span>
</div>

<div id="elm-group-form"></div>
<script src="/admin/assets/js/group-form.js"></script>
<script type="text/javascript">
  var app = Elm.GroupForm.embed(document.getElementById('elm-group-form'), {groupId: ${group.id!'null'}});
</script>

<select name="userId[]" class="selectpicker" multiple>
  <#list users as user>
  <option value="${user.id}"<#if userIds?seq_contains(user.id)> selected</#if>>${user.name}</option>
  </#list>
</select>
