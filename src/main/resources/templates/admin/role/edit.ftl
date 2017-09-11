<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit role">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Roles</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit role</h1>

   <form method="post" action="${urlFor('update?id=' + role.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
