<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit permission">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Permissions</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit permission</h1>

   <form method="post" action="${urlFor('update?id=' + permission.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
