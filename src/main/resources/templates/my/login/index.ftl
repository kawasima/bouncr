<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of users">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" method="post">
        <input type="text" name="account" class="form-control" placeholder="Account"/>
        <input type="password" name="password" class="form-control" placeholder="Password"/>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Sign in</button>
      </form>
    </div>
  </div>
</@layout.layout>
