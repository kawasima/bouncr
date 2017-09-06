<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit realm">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('listByApplicationId?applicationId=' + realm.applicationId)}">Realms</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
   <h1>Edit realm</h1>

   <form method="post" action="${urlFor('update?applicationId='+ realm.applicationId + '&id=' + realm.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
