/**
 * Copyright 2014 symentis GmbH
 * <p/>
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.symentis.cas.adaptors.ws;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.impl.callback.PasswordCallback;
import com.sun.xml.wss.impl.callback.UsernameCallback;
import org.springframework.core.io.Resource;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.IOException;


/**
 * Baseclass for Webservice Client implementations. Supports WSSE Header.
 *
 * @author Robert Oschwald
 */
public abstract class WebserviceClientBase extends WebServiceGatewaySupport {
  protected static XWSSProcessor _scProcessor;
  private Resource _securityConfigResource;


  /**
   * Inner callback class for adding WSSE security header to message.
   *
   * @author Robert Oschwald
   */
  public final class LocalWebServiceMessageCallback implements WebServiceMessageCallback {
    private final XWSSProcessor _processor;

    /**
     * Constructor.
     * @param processor
     */
    public LocalWebServiceMessageCallback(XWSSProcessor processor) {
      this._processor = processor;
    }

    /**
     * Execute any number of operations on the supplied <code>message</code>.
     *
     * @param message the message
     * @throws IOException          in case of I/O errors
     * @throws TransformerException in case of transformation errors
     */
    public void doWithMessage(WebServiceMessage message) {
      SaajSoapMessage ssm = (SaajSoapMessage) message;
      SOAPMessage sm = ssm.getSaajMessage();
      try {
        ProcessingContext context = this._processor.createProcessingContext(sm);
        SOAPMessage secureM = this._processor.secureOutboundMessage(context);
        ssm.setSaajMessage(secureM);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * inner class to modify WSSE username/pw in security config document dynamically
   */
  public final class SecurityConfigModifier implements CallbackHandler {
    private final String _username;
    private final String _password;

    /**
     * Constructor.
     * @param username
     * @param password
     */
    public SecurityConfigModifier(String username, String password) {
      this._username = username;
      this._password = password;
    }

    // handler
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
      for (int i = 0; i < callbacks.length; i++) {
        if (callbacks[i] instanceof UsernameCallback) {
          UsernameCallback callback = (UsernameCallback) callbacks[i];
          callback.setUsername(this._username);
        } else if (callbacks[i] instanceof PasswordCallback) {
          PasswordCallback callback = (PasswordCallback) callbacks[i];
          callback.setPassword(this._password);
        } else {
          throw new UnsupportedCallbackException(callbacks[i]);
        }
      }
    }
  }

  /**
   * Return SecurityConfigResource.
   *
   * @return SecurityConfigResource
   */
  public Resource getSecurityConfigResource() {
    return this._securityConfigResource;
  }

  /**
   * Set SecurityConfigResource.
   *
   * @param securityConfigResource
   */
  public void setSecurityConfigResource(Resource securityConfigResource) {
    this._securityConfigResource = securityConfigResource;
  }
}
