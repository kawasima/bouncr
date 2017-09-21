<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New realm">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.ApplicationController', 'list?id=' + application.id)}">Application: ${application.name}</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('listByApplicationId?applicationId=' + realm.applicationId)}">Realms</a></li>
    <li class="breadcrumb-item active">New</li>
  </ol>
   <h1>New realm</h1>

   <form method="post" action="${urlFor('create?applicationId=' + realm.applicationId)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
