<div class="form-group<#if realm.hasErrors("name")> has-error</#if>">
  <label for="name">Name</label>
  <input id="name" class="form-control" type="text" name="name" value="${realm.name!''}"/>
  <span class="help-block"><#if realm.hasErrors("name")>${realm.getErrors("name")?join(",")}</#if></span>
</div>
<div class="form-group<#if realm.hasErrors("description")> has-error</#if>">
  <label for="description">Description</label>
  <input id="description" class="form-control" type="text" name="description" value="${realm.description!''}"/>
  <span class="help-block"><#if realm.hasErrors("description")>${realm.getErrors("description")?join(",")}</#if></span>
</div>
<div class="form-group<#if realm.hasErrors("url")> has-error</#if>">
  <label for="description">URL</label>
  <input id="description" class="form-control" type="text" name="url" value="${realm.url!''}"/>
  <span class="help-block"><#if realm.hasErrors("description")>${realm.getErrors("url")?join(",")}</#if></span>
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
      <#list groups as group>
      <option value="">Select...</option>
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
