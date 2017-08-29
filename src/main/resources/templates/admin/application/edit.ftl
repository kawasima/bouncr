<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit application">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Applications</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit application</h1>

   <form method="post" action="${urlFor('update?id=' + application.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
