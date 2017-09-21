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
  <label for="scope">${t('field.sope')}</label>
  <input id="scope" class="form-control<#if oidcProvider.hasErrors("scope")> is-invalid</#if>" type="text" name="scope" value="${oidcProvider.scope!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("scope")>${oidcProvider.getErrors("scope")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("responseType")> has-error</#if>">
  <label for="responseType">${t('label.responseType')}</label>
  <input id="responseType" class="form-control<#if oidcProvider.hasErrors("responseType")> is-invalid</#if>" type="text" name="responseType" value="${oidcProvider.responseType!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("responseType")>${oidcProvider.getErrors("responseType")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("authorizationEndpoint")> has-error</#if>">
  <label for="authorizationEndpoint">${t('label.authorizationEndpoint')}</label>
  <input id="authorizationEndpoint" class="form-control<#if oidcProvider.hasErrors("authorizationEndpoint")> is-invalid</#if>" type="text" name="authorizationEndpoint" value="${oidcProvider.authorizationEndpoint!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("authorizationEndpoint")>${oidcProvider.getErrors("authorizationEndpoint")?join(",")}</#if></span>
</div>

<div class="form-group<#if oidcProvider.hasErrors("tokenEndpoint")> has-error</#if>">
  <label for="tokenEndpoint">${t('label.tokenEndpoint')}</label>
  <input id="tokenEndpoint" class="form-control<#if oidcProvider.hasErrors("tokenEndpoint")> is-invalid</#if>" type="text" name="tokenEndpoint" value="${oidcProvider.tokenEndpoint!''}"/>
  <span class="invalid-feedback"><#if oidcProvider.hasErrors("tokenEndpoint")>${oidcProvider.getErrors("tokenEndpoint")?join(",")}</#if></span>
</div>

