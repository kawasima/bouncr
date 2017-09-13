<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Sign up">
   <h1>Sign up</h1>

   <form method="post" action="${urlFor('create')}">
     <#include "_form.ftl">
     <button type="submit" class="btn btn-primary">Register</button>
   </form>
</@layout.layout>
