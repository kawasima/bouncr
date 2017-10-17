<div class="form-group<#if oidcProvider.hasErrors("name")> has-error</#if>">
  <label for="name">${t('field.name')}</label>
  <input id="name" class="form-control<#if oidcProvider.hasErrors("name")> is-invalid</#if>" type="text" name="name" value="${oidcProvider.name!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("name")>${oidcProvider.getErrors("name")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("apiKey")> has-error</#if>">
  <label for="apiKey">${t('field.apiKey')}</label>
  <input id="apiKey" class="form-control<#if oidcProvider.hasErrors("apiKey")> is-invalid</#if>" type="text" name="apiKey" value="${oidcProvider.apiKey!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("apiKey")>${oidcProvider.getErrors("apiKey")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("apiSecret")> has-error</#if>">
  <label for="apiSecret">${t('field.apiSecret')}</label>
  <input id="apiSecret" class="form-control<#if oidcProvider.hasErrors("apiSecret")> is-invalid</#if>" type="text" name="apiSecret" value="${oidcProvider.apiSecret!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("apiSecret")>${oidcProvider.getErrors("apiSecret")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("scope")> has-error</#if>">
  <label for="scope">${t('field.scope')}</label>
  <input id="scope" class="form-control<#if oidcProvider.hasErrors("scope")> is-invalid</#if>" type="text" name="scope" value="${oidcProvider.scope!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("scope")>${oidcProvider.getErrors("scope")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("responseType")> has-error</#if>">
  <label for="responseType">${t('field.responseType')}</label>
  <select name="responseType" class="form-control<#if oidcProvider.hasErrors("responseType")> is-invalid</#if>">
    <#list responseTypes as responseType>
    <option value="${responseType.getName()}"<#if oidcProvider.responseType == responseType.getName()>selected="selected"</#if>>${responseType.getName()}</option>
    </#list>
  </select>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("responseType")>${oidcProvider.getErrors("responseType")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("authorizationEndpoint")> has-error</#if>">
  <label for="authorizationEndpoint">${t('field.authorizationEndpoint')}</label>
  <input id="authorizationEndpoint" class="form-control<#if oidcProvider.hasErrors("authorizationEndpoint")> is-invalid</#if>" type="text" name="authorizationEndpoint" value="${oidcProvider.authorizationEndpoint!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("authorizationEndpoint")>${oidcProvider.getErrors("authorizationEndpoint")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("tokenEndpoint")> has-error</#if>">
  <label for="tokenEndpoint">${t('field.tokenEndpoint')}</label>
  <input id="tokenEndpoint" class="form-control<#if oidcProvider.hasErrors("tokenEndpoint")> is-invalid</#if>" type="text" name="tokenEndpoint" value="${oidcProvider.tokenEndpoint!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("tokenEndpoint")>${oidcProvider.getErrors("tokenEndpoint")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("tokenEndpointAuthMethod")> has-error</#if>">
  <label for="tokenEndpoint">${t('field.tokenEndpointAuthMethod')}</label>
  <select name="tokenEndpointAuthMethod" class="form-control<#if oidcProvider.hasErrors("tokenEndpointAuthMethod")> is-invalid</#if>">
    <#list tokenEndpointAuthMethods as tokenEndpointAuthMethod>
    <option value="${tokenEndpointAuthMethod.getValue()}"<#if oidcProvider.tokenEndpointAuthMethod == tokenEndpointAuthMethod.getValue()>selected="selected"</#if>>${tokenEndpointAuthMethod.getValue()}</option>
    </#list>
  </select>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("tokenEndpoint")>${oidcProvider.getErrors("tokenEndpoint")?join(",")}</#if></span>
</div>



