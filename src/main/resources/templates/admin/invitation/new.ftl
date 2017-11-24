<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New OAuth2 application">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('newForm')}">Invitation</a></li>
    <li class="breadcrumb-item active">New</li>
  </ol>
   <h1>New Invitation</h1>

   <form method="post" action="${urlFor('create')}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
