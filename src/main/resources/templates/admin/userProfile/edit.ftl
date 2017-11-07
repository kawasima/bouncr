<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit the field of user profile">
  <ol class="breadcrumb">
    <li class="breadcrumb-item"><a href="${urlFor('net.unit8.bouncr.web.controller.admin.IndexController', 'home')}">Administration</a></li>
    <li class="breadcrumb-item"><a href="${urlFor('list')}">User profile field</a></li>
    <li class="breadcrumb-item active">Edit</li>
  </ol>
  <h1>Edit the field of user profile</h1>

  <form method="post" action="${urlFor('update?id=' + userProfileField.id)}">
    <#include "_form.ftl">
    <button type="submit" class="btn btn-primary">Update</button>
  </form>
</@layout.layout>
