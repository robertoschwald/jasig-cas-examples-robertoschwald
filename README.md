JASIG CAS EXAMPLE EXTENSIONS
============================

Example APEREO CAS WAR Overlay project with various CAS extension modules 

**Using JPA for Ticket- and Service Registry (HSQLDB)**

Example extensions to the standard JASIG CAS SSO Server by symentis GmbH, Robert Oschwald.

CAS-Server Version
------------------
This project currently supports **CAS 4.1.3-SNAPSHOT**.

For older versions, see the corresponding branches.

CAS Overlay
-----------
This project is using the Apereo CAS *Maven overlay* mechanism: [http://jasig.github.io/cas/4.1.x/installation/Maven-Overlay-Installation.html](http://jasig.github.io/cas/4.1.x/installation/Maven-Overlay-Installation.html).  

It's composed of two war overlays:

- the *cas-server-overlay* module creates the CAS server webapp war.
- the *cas-management-overlay* module creates the CAS services management webapp war.

JPA Configuration
-----------------
This demo application uses JPA for the Service- and Ticket Registries.

Service-Registry config is done in cas-server-overlay and in cas-management-overlay, as both share the same db tables for service registry entries.

This example uses HSQLDB. Real implementations use MySQL, PostgreSQL, Oracle, MSSQL or any other Hibernate supported database backend.

Extensions
----------
This project additionally holds the following CAS Server extensions

WebserviceAuthenticationHandler
-----------------------------
Module cas-server-support-webservice is a sample WebserviceAuthenticationHandler implementation you can use to authenticate
against a Webservice based backend. The WebserviceAuthenticationHandler is webservice technology agnostic.

Simply wire in your WebserviceClient implementation (e.g. SOAP or REST client).

Provided is a Spring-WS based Webservice client which can be configured to run with- or without a WSSE header.

DirectMappedPersonAttributeDao
----------------------------
By default, the PersonAttributeDao implementations of the Jasig Person-Directory library need an extra request
after sucessful authentication to pull user attributes to provide them to CAS Client applications.

The DirectMappedPersonAttributeDao is a short-term caching attributeRepository, which can be filled with user attributes
from beans directly (e.g. by AuthenticationHandlers).

This example CAS Server application wires the DirectMappedPersonAttributeDao into the WebserviceAuthenticationHandler.

On successful authentication, the received principal attributes are stored in the DirectMappedPersonAttributeDao.
On first serviceValidate state, the attributes for the principal are removed from the short-term cache.
Stale entries (e.g. no serviceValidate state happened) are removed after a TTL (default 1 minute).
See the provided deployerConfigContext.xml file for an example configuration of this attributeRepository.

Note: The current version of the DirectMappedPersonAttributeDao is netId case-sensitive. So either ensure the netId gets converted to lowercase (e.g. in UsernamePasswordCredentials) or convert the netId in the DirectMappedPersonAttributeDao methods to lowercase, as the netId is used as the key in the attribute cache map.

Example serviceValidate response:

```xml
<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
        <cas:user>admin</cas:user>
        <cas:attributes>
            <cas:isRemembered></cas:isRemembered>
            <cas:isFromNewLogin>false</cas:isFromNewLogin>
                <cas:USER_ATTRIB_CACHE_EXPIRY_TIME>1397730496188</cas:USER_ATTRIB_CACHE_EXPIRY_TIME>
                <cas:lastname>TestLastName</cas:lastname>
                <cas:netid>admin</cas:netid>
                <cas:firstname>TestFirstName</cas:firstname>
        </cas:attributes>
    </cas:authenticationSuccess>
</cas:serviceResponse>
```

To prevent the USER_ATTRIB_CACHE_EXPIRY_TIME attribute to be returned, do not select the "Ignore Attribute Management via this Tool" checkbox in the service management application.
Instead, select the attributes you want to have returned to services on a per-service base.

Project setup
-------------
After you checked out the code from the repository, it is mandatory that you perform:

```
mvn install
```

This creates the JaxB classes for the sample SOAP webservice in target/src and the artifacts needed.

Sample application
------------------
The CAS application maven overlay configuration in the cas-server-overlay module of this project uses

 * Spring-WS based WebServiceClient to authenticate against a provided test Spring-WS WebserviceEndpoint (which btw. accepts every username / password given).
   The WebserviceClient is configured to not use a WSSE header.
 * Configures the DirectMappedPersonAttributeDao to provide CAS Client applications user attributes received by the WebserviceClient.

Configuration
-------------
 * cas-server-overlay/src/main/webapp/WEB-INF/deployerConfigContext.xml
 * cas-server-overlay/src/main/webapp/WEB-INF/spring-ws-config.xml
 * cas-server-overlay/src/main/webapp/WEB-INF/spring-configuration
 * cas-server-overlay/src/main/webapp/WEB-INF/webservice-configuration
 * cas-server-overlay/src/main/webapp/WEB-INF/web.xml
   This is the original CAS Server 4.1.3-SNAPSHOT web.xml file plus Spring-WS MessageDispatcherServlet config at the bottom, added for the test Spring-WS ExampleAuthenticationEndpoint.
 * cas-server-overlay/src/main/webapp/view/jsp/protocol/casServiceValidationSuccess.jsp (adds the cas attributes to the CAS 2.0 service response as a custom extension (by default, attributes are only supported at the CAS 3.0 Spec default URI /p3/serviceValidate).
 * cas-management-overlay/src/main/resources/user-details.properties (configure usernames allowed to access the management webapp)


Running the sample CAS application
----------------------------------
The project contains the "jetty" maven plugin which provides a quick self-contained
[APEREO CAS](https://www.apereo.org/projects/cas) demo server environment, performing the following:
 * Downloads the CAS and CAS-Management project war file maven artifacts and overlays them with the corresponding local modifications of this project.
 * Launch CAS and CAS-Management webapps on an embedded Jetty instance (ports 8080 and 8443)
 * Grants access to the 'testadmin' account for the Services Management interface.

Thanks to the work of [Jerome Leleu](https://github.com/leleuj/cas-overlay-demo). 

Running the Jetty application server:

```
 cd cas-server-overlay
 mvn jetty:run
```

Then access https://localhost:8443/cas/ in your favorite browser.

The Services Management webapp can be accessed at

 http://localhost:8080/cas-management. (log in as testadmin:<anypassword>)

Warning
-------
*DO NOT USE THIS PROJECT AS PART OF ANY PRODUCTION BUILD*.
Instead, use a separate Java application server (Tomcat, JBoss, etc), properly secured,
and build a securely configured CAS server bundle to be deployed into that app server.

Keystore files
--------------
Apereo CAS and the Webservice Client rely on HTTPS. Therefore, the maven build creates a SSL Certificate and
stores it in a Java keystore file used by the embedded Jetty server, so the underlying Java implementation trusts this
self-signed certificate (The WebserviceClient otherwise would not accept the connection)

 * The JKS and PEM files are available in cas-server-overlay/target/jetty-ssl.keystore. (Useful for testing external CAS clients as well)
 * CAS server logs are in cas-server-overlay/target/

CAS URLs
--------
* Login: https://localhost:8443/cas/login
* Services Management: https://localhost:8443/cas/services/manage.html
* CAS 2.0 Service Validation: https://localhost:8443/cas/serviceValidate

More Information
----------------
 * [APEREO CAS](https://www.apereo.org/projects/cas)
 * [APEREO CAS on GitHub](https://github.com/jasig/cas)


Deploy CAS 5.x Howto
--------------------
https://dacurry-tns.github.io/deploying-apereo-cas/high-avail_distrib-metadata_overview.html

LICENSE
-------
Copyright 2015 symentis GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.



