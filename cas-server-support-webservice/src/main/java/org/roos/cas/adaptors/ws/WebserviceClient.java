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
package org.roos.cas.adaptors.ws;

import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;

/**
 * WebserviceClient.
 * <P>
 * Creation date: 15.04.2014 01:35:33
 * <P>
 * @author Robert Oschwald
 */
public interface WebserviceClient {
	
	/**
	 * perform the Authentication
	 * <P>
	 * Creation date: 15.04.2014 01:35:37
   * <P>
	 * @param credentials 
	 * @return {@link SimplePrincipal}
	 * @throws AuthenticationException 
	 */
	public SimplePrincipal doAuthentication(final UsernamePasswordCredentials credentials) throws AuthenticationException;
}
