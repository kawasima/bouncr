<#import "../../layout/defaultLayout.ftl" as layout>
<#assign editMode=false>
<@layout.layout "New user">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">Users</a></li>
    <li class="breadcrumb-item active">New</li>
  </ol>
   <h1>New user</h1>

   <form method="post" action="${urlFor('create')}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
