<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit realm">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.ApplicationController', 'list?id=' + application.id)}">Application: ${application.name}</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('listByApplicationId?applicationId=' + application.id)}">Realms</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit realm</h1>

   <form method="post" action="${urlFor('update?applicationId='+ application.id + '&id=' + realm.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
