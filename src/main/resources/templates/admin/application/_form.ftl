     <div class="form-group<#if application.hasErrors("name")> has-danger</#if>">
       <label for="name">Name</label>
       <input id="name" class="form-control<#if application.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${application.name!''}"/><#if application.hasErrors("name")!false>${application.getErrors("name")?join(",")}</#if>
       <span class="invalid-feedback"><#if applicaion.hasErrors("name")>${application.getErrors("name")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("description")> has-danger</#if>">
       <label for="description">Description</label>
       <input id="description" class="form-control<#if application.hasErrors("description")> is-invalid</#if>" type="text" name="description" value="${application.description!''}"/>
       <span class="invalid-feedback"><#if application.hasErrors("description")>${application.getErrors("description")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("passTo")> has-danger</#if>">
       <label for="passTo">Pass to</label>
       <input id="passTo" class="form-control<#if application.hasErrors("passTo")> is-invalid</#if>" type="text" name="passTo" value="${application.passTo!''}"/>
       <span class="invalid-feedback"><#if application.hasErrors("passTo")>${application.getErrors("passTo")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("virtualPath")> has-danger</#if>">
       <label for="virtualPath">Virtual path</label>
       <input id="virtualPath" class="form-control<#if application.hasErrors("virtualPath")> is-invalid</#if>" type="text" name="virtualPath" value="${application.virtualPath!''}"/>
       <span class="invalid-feedback"><#if application.hasErrors("virtualPath")>${application.getErrors("virtualPath")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("topPage")> has-danger</#if>">
       <label for="topPage">Top page url</label>
       <input id="topPage" class="form-control<#if application.hasErrors("topPage")> is-invalid</#if>" type="text" name="topPage" value="${application.topPage!''}"/>
       <span class="invalid-feedback"><#if application.hasErrors("topPage")>${application.getErrors("topPage")?join(",")}</#if></span>
     </div>

