<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit user">
   <h1>Edit user</h1>

   <form method="post" action="${urlFor('update?id=' + user_id)}">
     <div class="form-group">
       <label for="name">Account</label>
       ${user.name!''}
     </div>
     <div class="form-group<#if user.hasErrors("password")> has-error</#if>">
       <label for="password">Password</label>
       <input id="password" class="form-control" type="password" name="password" value=""/>
       <span class="help-block"><#if user.hasErrors("password")>${user.getErrors("password")?join(",")}</#if></span>
     </div>
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
