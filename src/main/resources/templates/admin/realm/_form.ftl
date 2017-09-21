<div class="form-group<#if realm.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <#if writeProtected>
  ${realm.name}
  <input type="hidden" name="name" value="${realm.name}"/>
  <#else>
  <input id="name" class="form-control<#if realm.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${realm.name!''}"/>
  <span class="invalid-feedback"><#if realm.hasErrors("name")>${realm.getErrors("name")?join(",")}</#if></span>
  </#if>
</div>
<div class="form-group<#if realm.hasErrors("description")> has-error</#if>">
  <label for="description">${t('field.description')}</label>
  <#if writeProtected>
  ${realm.description}
  <input type="hidden" name="description" value="${realm.description}"/>
  <#else>
  <input id="description" class="form-control<#if realm.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${realm.description!''}"/>
  <span class="invalid-feedback"><#if realm.hasErrors("description")>${realm.getErrors("description")?join(",")}</#if></span>
  </#if>
</div>
<div class="form-group<#if realm.hasErrors("url")> has-error</#if>">
  <label for="url">URL</label>
  <#if writeProtected>
  ${realm.url}
  <input type="hidden" name="url" value="${realm.url}"/>
  <#else>
  <div class="input-group">
    <span class="input-group-addon" id="url-addon">${application.virtualPath}/</span>
    <input id="url" class="form-control<#if realm.hasErrors("url")> is-invalid</#if>" type="text" name="url" value="${realm.url!''}" aria-describedby="url-addon"/>
  </div>
  <span class="invalid-feedback"><#if realm.hasErrors("url")>${realm.getErrors("url")?join(",")}</#if></span>
  </#if>
</div>

<#list realm.assignments as assignment>
  <div class="form-group<#if realm.hasErrors("groupId")> has-error</#if>">
    <label for="Group">Group</label>
    <select name="assignments[][groupId]" class="form-control">
      <option value="">Select...</option>
      <#list groups as group>
      <option value="${group.id}"<#if assignment.groupId == group.id> selected="selected"</#if>>${group.name}</option>
      </#list>
    </select>
  </div>

  <div class="form-group<#if realm.hasErrors("roleId")> has-error</#if>">
    <label for="roleId">Role</label>
    <select name="assignments[][roleId][]" class="form-control" multiple="multiple">
      <#list roles as role>
      <option value="${role.id}"<#if assignment.roleId?seq_contains(role.id)> selected="selected"</#if>>${role.name}</option>
      </#list>
    </select>
  </div>
</#list>

  <div class="form-group<#if realm.hasErrors("groupId")> has-error</#if>">
    <label for="Group">Group</label>
    <select name="assignments[][groupId]" class="form-control">
      <option value="">Select...</option>
      <#list groups as group>
      <option value="${group.id}">${group.name}</option>
      </#list>
    </select>
  </div>

  <div class="form-group<#if realm.hasErrors("roleId")> has-error</#if>">
    <label for="roleId">Role</label>
    <select name="assignments[][roleId][]" class="form-control" multiple="multiple">
      <#list roles as role>
      <option value="${role.id}">${role.name}</option>
      </#list>
    </select>
  </div>
