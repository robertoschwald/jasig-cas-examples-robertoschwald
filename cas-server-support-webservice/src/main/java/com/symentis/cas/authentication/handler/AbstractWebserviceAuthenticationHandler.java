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

package com.symentis.cas.authentication.handler;

import com.symentis.cas.adaptors.ws.WebserviceClient;
import org.jasig.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;

import javax.validation.constraints.NotNull;


/**
 * Abstract class for webservice authentication handlers.
 *
 * @author Robert Oschwald
 */

public abstract class AbstractWebserviceAuthenticationHandler extends
  AbstractUsernamePasswordAuthenticationHandler {

  @NotNull
  protected WebserviceClient _webserviceClient;

  /**
   * webserviceClient.
   *
   * @param webserviceClient the Handler uses
   */
  public final void setWebserviceClient(final WebserviceClient webserviceClient) {
    this._webserviceClient = webserviceClient;
  }
}
