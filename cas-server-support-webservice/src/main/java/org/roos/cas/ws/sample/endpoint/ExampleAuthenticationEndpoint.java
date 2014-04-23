/**
 * Copyright 2014 symentis GmbH
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

package org.roos.cas.ws.sample.endpoint;

/**
 * Simple Spring WS Soap endpoint to test the WebserviceAuthenticationHandler against.
 */
import org.apache.commons.lang.StringUtils;
import org.roos.cas.ws.samples.auth.schema.AuthRequest;
import org.roos.cas.ws.samples.auth.schema.AuthResponse;
import org.roos.cas.ws.samples.auth.schema.ObjectFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Sample Authentication Spring WS Endpoint.
 */
@Endpoint
public class ExampleAuthenticationEndpoint {

  @PayloadRoot(namespace = "http://roos.cas.samples.ws.org/auth", localPart = "authRequest")
  @ResponsePayload
  public AuthResponse authenticate(@RequestPayload AuthRequest request) throws Exception {
    AuthResponse response = new ObjectFactory().createAuthResponse();

    if (StringUtils.isBlank(request.getNetid()) || StringUtils.isBlank(request.getPassword())){
      throw new Exception("netid and password mandatory.");
    }
    // attributes
    response.setNetid(request.getNetid());
    response.setLastname("TestLastName");
    response.setFirstname("TestFirstName");
    return response;
  }
}
