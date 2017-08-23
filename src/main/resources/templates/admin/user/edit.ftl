<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit user">
   <h1>Edit user</h1>

   <form method="post" action="${urlFor('update?id=' + userId)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
