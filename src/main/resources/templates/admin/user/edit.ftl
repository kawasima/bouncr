<#import "../../layout/defaultLayout.ftl" as layout>
<#assign editMode=true>
<@layout.layout "Edit user">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Users</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit user</h1>

   <form method="post" action="${urlFor('update?id=' + user.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
