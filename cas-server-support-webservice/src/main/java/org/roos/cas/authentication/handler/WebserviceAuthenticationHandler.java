/**
 * Copyright 2014 symentis GmbH
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

package org.roos.cas.authentication.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;

import org.roos.cas.services.persondir.support.DirectMappedPersonAttributeDao;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * Authentication Handler for Webservice Based Authentications.
 * You must inject the WebserviceClient by Spring config.
 *
 * Directly updates users attributes if injected DirectMappedPersonAttributeDao.
 *
 * @author Robert Oschwald
 */
public class WebserviceAuthenticationHandler extends AbstractWebserviceAuthenticationHandler implements InitializingBean {
  private static Log log = LogFactory.getLog(WebserviceAuthenticationHandler.class);
  private DirectMappedPersonAttributeDao attributeRepository;

  public final void setAttributeRepository(final DirectMappedPersonAttributeDao attributeRepository) {
    this.attributeRepository = attributeRepository;
  }

  public void afterPropertiesSet() throws Exception {
    // stub
  }

  /**
   * Authenticate user using webserviceClient.
   * Throws a TesteeAuthenticationException if the useraccount is disabled.
   *
   * @param credentials
   * @return true if sucessfully authenticated, otherwise false.
   * @throws org.jasig.cas.authentication.handler.AuthenticationException
   * @see org.jasig.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler#authenticateUsernamePasswordInternal(org.jasig.cas.authentication.principal.UsernamePasswordCredentials)
   */
  @Override
  protected final boolean authenticateUsernamePasswordInternal(UsernamePasswordCredentials credentials) throws AuthenticationException {
    SimplePrincipal principal = this._webserviceClient.doAuthentication(credentials);
    if (principal != null) {
      updatePersonAttributes(principal);
      // Add Authorization checks if needed
      return true;
    }
    log.warn("Person received is null!");
    return false;
  }

  /*
  * Convert principalAttributes to personAttributes and update in attributeRepository
   */
  private void updatePersonAttributes(Principal principal) {
    if (attributeRepository == null) return; // not injected
    if (principal.getAttributes() == null) return;
    Map<String, Object> principalAttributes = principal.getAttributes();
    Map<String,List<Object>> personAttributes = new HashMap<String, List<Object>>();
    for (Map.Entry<String, Object> entry : principalAttributes.entrySet()) {
      List<Object> values = new ArrayList<Object>();
      values.add(entry.getValue());
      personAttributes.put(entry.getKey(), values);
    }
    attributeRepository.addAttributes(principal.getId(), personAttributes);
  }
}
