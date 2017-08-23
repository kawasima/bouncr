<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New realm">
   <h1>New realm</h1>

   <form method="post" action="${urlFor('create?applicationId=' + realm.applicationId)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
