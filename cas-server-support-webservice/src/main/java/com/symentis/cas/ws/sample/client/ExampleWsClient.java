/**
 * Copyright 2014 symentis GmbH
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.symentis.cas.ws.sample.client;

import com.symentis.cas.adaptors.ws.WebserviceClientBase;
import com.symentis.cas.ws.samples.auth.schema.AuthRequest;
import com.symentis.cas.ws.samples.auth.schema.AuthResponse;
import com.symentis.cas.ws.samples.auth.schema.ObjectFactory;
import org.apache.commons.lang.StringUtils;

import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.authentication.principal.DefaultPrincipalFactory;
import org.jasig.cas.authentication.principal.Principal;
import com.symentis.cas.adaptors.ws.WebserviceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.ServletContextResource;

import javax.servlet.ServletContext;
import javax.validation.constraints.NotNull;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Example CAS SOAP Spring-WS Webservice Client using JaxB and optional WSSE header support.
 * For static username password setup, set wsUsername and wsPass in the bean def,
 * otherwise the given users' credentials username/password are used.
 * <p/>
 * Creation date: 15.04.2014 01:42:35
 * <p/>
 *
 * @author Robert Oschwald
 */
@Configurable
public class ExampleWsClient extends WebserviceClientBase implements WebserviceClient {
  private static final Logger log = LoggerFactory.getLogger(ExampleWsClient.class);
  private ServletContext servletContext;
  private Resource _sConfigResource;
  // optional static username/pw from bean context definition. See setters.
  private String _wsUsername;
  private String _wsPass;
  private String _username;
  private String _password;
  private boolean _useWSSE;
  @NotNull
  private String configFilePath;

  /**
   * Perform authentication.
   * <p/>
   * Creation date: 15.04.2014 05:01:05
   * <p/>
   *
   * @param credential The given user credential
   * @return SimplePrincipal
   * @throws GeneralSecurityException
   *
   */
  public Principal doAuthentication(UsernamePasswordCredential credential) throws GeneralSecurityException {
    return authenticateUser(credential);
  }

  private Principal authenticateUser(UsernamePasswordCredential credential) throws GeneralSecurityException {
    log.warn("SOAP authenticate User: " + credential.getUsername());
    ObjectFactory factory = new ObjectFactory();
    AuthRequest request = factory.createAuthRequest();
    AuthResponse response;
    // configure parameters
    this.configureParameters(credential);
    request.setNetid(this._username);
    request.setPassword(this._password);
    try {
      if (_useWSSE) {
        // With dynamic WSSE header. Adding the given username / pw as a WSSE header
        SecurityConfigModifier securityConfigCallbackHandler = new SecurityConfigModifier(this._username, this._password);
        // processor with callback to dynamically add username/password.
        XWSSProcessorFactory pf = XWSSProcessorFactory.newInstance();
        final XWSSProcessor processor = pf.createProcessorForSecurityConfiguration(
            this._sConfigResource.getInputStream(), securityConfigCallbackHandler);
        // add WSSE security header to message via CallBack Handler.
        // Would be much easier with SpringWS 2.5.x ...
        response = (AuthResponse) getWebServiceTemplate().marshalSendAndReceive(
            getDefaultUri(),
            request,
            new LocalWebServiceMessageCallback(processor));
      } else {
        log.warn("send soap request: " + request);
        response = (AuthResponse) getWebServiceTemplate().marshalSendAndReceive(getDefaultUri(), request);
      }
    } catch (Exception e) {
      log.error("Error:", e);
      return null;
    }
    log.debug("SOAP Response: " + response);
    if (response == null) return null;
    if (StringUtils.isNotBlank(response.getNetid())) {
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put("firstname", response.getFirstname());
      attributes.put("lastname", response.getLastname());
      attributes.put("netid", response.getNetid());
      return new DefaultPrincipalFactory().createPrincipal(this._username, attributes);
    }
    return null;
  }

  /**
   * Prepare client call
   * Uses username/password if set in the bean definition. Otherwise, use the given credentials
   * Also prepares the securityConfigResource.
   */
  private void configureParameters(UsernamePasswordCredential credential) {
    if (this._sConfigResource == null){
      this._sConfigResource = new ServletContextResource(servletContext, this.configFilePath);
    }
    if (StringUtils.isNotBlank(this._wsUsername)) {
      // got username/pw from bean definition
      this._username = this._wsUsername;
      this._password = this._wsPass;
    } else {
      // get username/pw from credentials.
      this._username = credential.getUsername();
      this._password = credential.getPassword();
    }
    // ensure that username is lowercase
    if (this._username != null) {
      this._username = this._username.trim().toLowerCase();
    }
  }

  /**
   * Optional: Static username for SOAP Request (WSSE Header)
   * @param wsUsername The SOAP Username
   */
  public void setWsUsername(String wsUsername) {
    this._wsUsername = wsUsername;
  }

  /**
   * Optional: Static password for SOAP Request (WSSE Header)
   * @param wsPass The SOAP Username
   */
  public void setWsPass(String wsPass) {
    this._wsPass = wsPass;
  }

  /* Set to true to modify outgoing payload to add a WSSE header */
  public void setUseWSSE(boolean useWSSE) {
    this._useWSSE = useWSSE;
  }

  /**
   * setter for WsSecurity config file path
   * <p/>
   * Creation date: 26.11.2015 09:48:30
   * <p/>
   */
  public void setConfigFilePath(String configFilePath) {
    this.configFilePath = configFilePath;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }
}
