<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New OpenId connect provider">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">OpenId connect providers</a></li>
    <li class="breadcrumb-item active">New</li>
  </ol>
   <h1>New OpenId Connect provider</h1>

   <form method="post" action="${urlFor('create')}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
