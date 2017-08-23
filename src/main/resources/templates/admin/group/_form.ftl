     <div class="form-group<#if group.hasErrors("name")> has-error</#if>">
       <label for="name">Name</label>
       <input id="name" class="form-control" type="text" name="name" value="${group.name!''}"/>
       <span class="help-block"><#if group.hasErrors("name")>${group.getErrors("name")?join(",")}</#if></span>
     </div>
     <div class="form-group<#if group.hasErrors("description")> has-error</#if>">
       <label for="description">Description</label>
       <input id="description" class="form-control" type="text" name="description" value="${group.description!''}"/>
       <span class="help-block"><#if group.hasErrors("description")>${group.getErrors("description")?join(",")}</#if></span>
     </div>
