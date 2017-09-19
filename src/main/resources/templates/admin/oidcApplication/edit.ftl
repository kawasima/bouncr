<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit OAuth2 application">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">OAuth2 Applications</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit OAuth2 application</h1>

   <form method="post" action="${urlFor('update?id=' + oauth2Application.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
