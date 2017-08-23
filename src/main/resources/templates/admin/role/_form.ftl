<div class="form-group<#if role.hasErrors("name")> has-error</#if>">
    <label for="name">Name</label>
    <input id="name" class="form-control" type="text" name="name" value="${role.name!''}"/>
    <span class="help-block"><#if role.hasErrors("name")>${role.getErrors("name")?join(",")}</#if></span>
</div>
    <div class="form-group<#if role.hasErrors("description")> has-error</#if>">
    <label for="description">Description</label>
    <input id="description" class="form-control" type="text" name="name" value="${role.description!''}"/>
    <span class="help-block"><#if role.hasErrors("description")>${role.getErrors("description")?join(",")}</#if></span>
</div>
