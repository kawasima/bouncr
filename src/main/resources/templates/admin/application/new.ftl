<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New application">
   <h1>New application</h1>

   <form method="post" action="${urlFor('create')}">
     <div class="form-group<#if application.hasErrors("name")> has-danger</#if>">
       <label for="name">Name</label>
       <input id="name" class="form-control" type="text" name="name" value="${application.name!''}"/><#if application.hasErrors("name")!false>${application.getErrors("name")?join(",")}</#if>
     </div>

     <div class="form-group<#if application.hasErrors("description")> has-danger</#if>">
       <label for="description">Description</label>
       <input id="description" class="form-control" type="text" name="description" value="${application.description!''}"/>
       <span class="help-block"><#if application.hasErrors("description")>${user.getErrors("description")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("passTo")> has-danger</#if>">
       <label for="passTo">Pass to</label>
       <input id="passTo" class="form-control" type="text" name="passTo" value="${application.passTo!''}"/>
       <span class="help-block"><#if application.hasErrors("passTo")>${user.getErrors("passTo")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("virtualPath")> has-danger</#if>">
       <label for="virtualPath">Virtual path</label>
       <input id="virtualPath" class="form-control" type="text" name="virtualPath" value="${application.virtualPath!''}"/>
       <span class="help-block"><#if application.hasErrors("virtualPath")>${user.getErrors("virtualPath")?join(",")}</#if></span>
     </div>

     <div class="form-group<#if application.hasErrors("topPage")> has-danger</#if>">
       <label for="topPage">Top page url</label>
       <input id="topPage" class="form-control" type="text" name="topPage" value="${application.topPage!''}"/>
       <span class="help-block"><#if application.hasErrors("topPage")>${user.getErrors("topPage")?join(",")}</#if></span>
     </div>

     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
