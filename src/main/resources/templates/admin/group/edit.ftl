<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit group">
   <h1>Edit group</h1>

   <form method="post" action="${urlFor('update?id=' + group.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
