<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New group">
   <h1>New group</h1>

   <form method="post" action="${urlFor('create')}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
