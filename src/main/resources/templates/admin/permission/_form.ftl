     <div class="form-group<#if permission.hasErrors("name")> has-error</#if>">
       <label for="name">Name</label>
       <input id="name" class="form-control" type="text" name="name" value="${permission.name!''}"/>
       <span class="help-block"><#if permission.hasErrors("name")>${permission.getErrors("name")?join(",")}</#if></span>
     </div>
     <div class="form-group<#if permission.hasErrors("description")> has-error</#if>">
       <label for="description">Name</label>
       <input id="description" class="form-control" type="text" name="name" value="${permission.description!''}"/>
       <span class="help-block"><#if permission.hasErrors("description")>${permission.getErrors("description")?join(",")}</#if></span>
     </div>
