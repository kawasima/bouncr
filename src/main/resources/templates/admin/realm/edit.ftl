<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Edit realm">
   <h1>Edit realm</h1>

   <form method="post" action="${urlFor('update?applicationId='+ realm.applicationId + '&id=' + realm.id)}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Update</button>
   </form>
</@layout.layout>
