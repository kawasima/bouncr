<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of groups">
  <h1>List of groups</h1>

  <#list groups>
  <table>
    <thead>
      <tr>
        <th>Name</th>
      </tr>
    </thead>
    <tbody>
      <#items as group>
        <tr>
          <td><a href="/group/${group.id}">${group.name}</a></td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No groups</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>
</@layout.layout>
