<#import "../../layout/defaultLayout.ftl" as layout>
<@layout.layout "List of certs">
  <h1>Certificates</h1>

  <#list certs>
  <table class="table">
    <thead>
      <tr>
        <th>${t('field.serial')}</th>
        <th>${t('field.expires')}</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <#items as certs>
        <tr>
          <td>${cert.serial}</td>
          <td>${cert.expires}</td>
          <td>
            <div class="btn-group">
              <form action="${urlFor('download?id=' + cert.id)}" method="post">
                <button class="btn" type="submit">${t('label.download')}</button>
              </form>
              <form action="${urlFor('delete?id=' + cert.id)}" method="post">
                <button class="btn" type="submit">${t('label.delete')}</button>
              </form>
            </div>
          </td>
        </tr>
      </#items>
    </tbody>
  </table>
  <#else>
  <div class="alert alert-info" role="alert">
     <p>No certs</p>
  </div>
  </#list>

  <a class="btn" href="${urlFor('newForm')}">Create</a>
</@layout.layout>
