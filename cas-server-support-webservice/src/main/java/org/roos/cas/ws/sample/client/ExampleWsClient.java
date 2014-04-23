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

package org.roos.cas.ws.sample.client;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.roos.cas.adaptors.ws.WebserviceClient;
import org.roos.cas.adaptors.ws.WebserviceClientBase;
import org.roos.cas.ws.samples.auth.schema.AuthRequest;
import org.roos.cas.ws.samples.auth.schema.AuthResponse;
import org.roos.cas.ws.samples.auth.schema.ObjectFactory;
import org.springframework.core.io.Resource;

import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Example CAS Webservice Client using JaxB and optional WSSE header support.
 * For static username password setup, set wsUsername and wsPass in the bean def,
 * otherwise the given users' credentials username/password are used.
 * <p/>
 * Creation date: 15.04.2014 01:42:35
 * <p/>
 *
 * @author Robert Oschwald
 */

public class ExampleWsClient extends WebserviceClientBase implements WebserviceClient {
  private static Log log = LogFactory.getLog(ExampleWsClient.class);
  private String _wsUsername;
  private String _wsPass;
  private String _username;
  private String _password;
  private boolean _useWSSE;
  private Resource _sConfigResource;

  /**
   * Perform authentication.
   * <p/>
   * Creation date: 15.04.2014 05:01:05
   * <p/>
   *
   * @param credentials
   * @return SimplePrincipal
   * @throws org.jasig.cas.authentication.handler.AuthenticationException
   *
   */
  public SimplePrincipal doAuthentication(UsernamePasswordCredentials credentials) throws AuthenticationException {
    return authenticateUser(credentials);
  }

  private SimplePrincipal authenticateUser(UsernamePasswordCredentials credentials) throws AuthenticationException {
    ObjectFactory factory = new ObjectFactory();
    AuthRequest request = factory.createAuthRequest();
    AuthResponse response;
    // configure parameters
    this.configureParameters(credentials);
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
        response = (AuthResponse) getWebServiceTemplate().marshalSendAndReceive(getDefaultUri(), request);
      }
    } catch (Exception e) {
      log.info(e);
      return null;
    }
    if (response == null) return null;
    if (StringUtils.isNotBlank(response.getNetid())) {
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put("firstname", response.getFirstname());
      attributes.put("lastname", response.getLastname());
      attributes.put("netid", response.getNetid());
      return new SimplePrincipal(this._username, attributes);
    }
    return null;
  }

  /**
   * Prepare client call
   * Uses username/password if set in the bean definition. Otherwise, use the given credentials
   * Also prepares the securityConfigResource.
   */
  private void configureParameters(UsernamePasswordCredentials credentials) {
    this._sConfigResource = this.getSecurityConfigResource();
    if (this._sConfigResource == null) {
      throw new RuntimeException("securityConfigResource not set.");
    }
    if (StringUtils.isNotBlank(this._wsUsername)) {
      // got username/pw from bean definition
      this._username = this._wsUsername;
      this._password = this._wsPass;
    } else {
      // get username/pw from credentials.
      this._username = credentials.getUsername();
      this._password = credentials.getPassword();
    }
    // ensure that username is lowercase
    if (this._username != null) {
      this._username = this._username.trim().toLowerCase();
    }
  }

  public void setWsUsername(String wsUsername) {
    this._wsUsername = wsUsername;
  }

  public void setWsPass(String wsPass) {
    this._wsPass = wsPass;
  }

  /* Set to true to modify outgoing payload to add a WSSE header */
  public void setUseWSSE(boolean useWSSE) {
    this._useWSSE = useWSSE;
  }

}
