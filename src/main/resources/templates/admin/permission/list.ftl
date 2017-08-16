<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of permissions">
  <h1>List of permissions</h1>

  <#list permissions>
  <table>
    <thead>
      <tr>
        <th>Name</th>
      </tr>
    </thead>
    <tbody>
      <#items as permission>
        <tr>
          <td><a href="/permission/${permission.id}">${permission.name}</a></td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No permissions</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>
</@layout.layout>
