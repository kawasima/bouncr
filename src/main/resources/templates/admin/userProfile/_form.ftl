<div class="form-group<#if userProfileField.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" name="name" class="form-control<#if userProfileField.hasErrors("name")> is-invalid</#if>" value="${userProfileField.name!''}"/>
  <span class="invalid-feedback"><#if userProfileField.hasErrors("name")>${userProfileField.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if userProfileField.hasErrors("jsonName")> has-error</#if>">
  <label for="json-name">JSON name</label>
  <input id="json-name" name="jsonName" class="form-control<#if userProfileField.hasErrors("name")> is-invalid</#if>" value="${userProfileField.jsonName!''}"/>
  <span class="invalid-feedback"><#if userProfileField.hasErrors("jsonName")>${userProfileField.getErrors("jsonName")?join(",")}</#if></span>
</div>

<div class="form-check">
  <label class="form-check-label">
    <input id="required" class="form-check-input" name="required" type="checkbox" value="true"
       <#if userProfileField.required>checked="checked"</#if>>
      Required?
  </label>
</div>

<div class="form-check">
  <label class="form-check-label">
    <input id="identity" class="form-check-input" name="identity" type="checkbox" value="true"
           <#if userProfileField.identity>checked="checked"</#if>>
      Identity?
  </label>
</div>
