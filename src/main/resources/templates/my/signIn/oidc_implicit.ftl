<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "Implicit flow">
  <script>
<!--
  var xhr = new XMLHttpRequest();
  xhr.onreadystatechange = function() {
    switch (xhr.readyState) {
    case 4:
      if(xhr.status == 200 || xhr.status == 304) {
        var data = JSON.parse(xhr.responseText);
        location.href = data.url
      } else {
        console.log('Failed. HttpStatus: ' + xhr.statusText);
      }
    }
  };
  xhr.open('POST', "${urlFor('signInByOidcImplicit?id=' + oidcProvider.id)}", false);
  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.send(document.location.hash.substr(1));
//-->
  </script>
</@layout.layout>
