<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of users">
  <h1>List of users</h1>

  <#list users>
  <table>
    <thead>
      <tr>
        <th>Name</th>
      </tr>
    </thead>
    <tbody>
      <#items as user>
        <tr>
          <td><a href="${urlFor('edit?id=' + user.id)}">${user.name}</a></td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No users</p>
  </div>
  </#list>

  <a href="${urlFor('newUser')}">New register</a>
</@layout.layout>
