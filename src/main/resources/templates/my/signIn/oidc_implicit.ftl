<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of users">
  <div class="container">
    <div class="card card-container">
      <img class="profile-img-card" class="profile-img-card" src="//ssl.gstatic.com/accounts/ui/avatar_2x.png"/>
      <form class="form-signin" action="${urlFor('signInByOidcImplicit')}" method="post">
        <input type="text" name="account" value="${signin.account}" class="form-control" placeholder="Account" readonly="readonly"/>
        <button class="btn btn-lg btn-primary btn-block btn-signin" type="submit">Sign In</button>
      </form>
    </div>
  </div>
  <script>
<!--
  var xhr = new XMLHttpRequest();
  xhr.onreadystatechange = function() {
    switch (xhr.readyState) {
    case 4:
      if(xhr.status == 200 || xhr.status == 304) {
        var data = xhr.responseText;
      } else {
        console.log('Failed. HttpStatus: ' + xhr.statusText);
      }
    }
  };
  xhr.open('POST', "${urlFor('signInByOidcImplicit?id=' + oidcProvider.id)}", false);
  xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
  xhr.send(document.location.hash.substr(1));
//-->
  </script>
</@layout.layout>
