<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "New user">
  <div class="alert alert-info">
  Finished signing up! <a class="btn btn-success" href="${urlFor('net.unit8.bouncr.web.controller.SignInController', 'signInForm')}">Sign In</a>
  </div>
</@layout.layout>
