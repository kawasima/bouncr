<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of applications">
  <h1>List of applications</h1>

  <#list applications>
  <table>
    <thead>
      <tr>
        <th>Name</th>
      </tr>
    </thead>
    <tbody>
      <#items as application>
        <tr>
          <td><a href="/application/${application.id}">${application.name}</a></td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No applications</p>
  </div>
  </#list>

  <a href="${urlFor('newForm')}">New register</a>
</@layout.layout>
