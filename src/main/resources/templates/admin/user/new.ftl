<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New user">
   <h1>New user</h1>

   <form method="post" action="${urlFor('create')}">
     <div class="form-group">
       <label for="name">Account</label>
       <input id="name" class="form-control" type="text" name="name" value="${user.name!''}"/><#if user.hasErrors("name")!false>${user.getErrors("name")?join(",")}</#if>
     </div>
     <div class="form-group<#if user.hasErrors("password")> has-error</#if>">
       <label for="password">Password</label>
       <input id="password" class="form-control" type="password" name="password" value=""/>
       <span class="help-block"><#if user.hasErrors("password")>${user.getErrors("password")?join(",")}</#if></span>
     </div>
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
