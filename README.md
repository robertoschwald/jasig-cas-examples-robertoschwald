JASIG CAS EXAMPLE OVERLAY
=========================

Example APEREO CAS WAR Overlay project with no custom extensions.

**Using JPA for Ticket- and Service Registry (HSQLDB)**

You can use this vanilla overlay project as a starting point for your own CAS Server implementation.

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

Project setup
-------------
After you checked out the code from the repository, it is mandatory that you perform:

```
mvn install
```

Configuration
-------------
 * cas-server-overlay/src/main/webapp/WEB-INF/deployerConfigContext.xml
 * cas-server-overlay/src/main/webapp/WEB-INF/spring-configuration
 * cas-server-overlay/src/main/webapp/view/jsp/protocol/casServiceValidationSuccess.jsp (adds the cas attributes to the CAS 2.0 service response as a custom extension. Standard CAS-Server 4.x supports attribute release only at the CAS 3.0 Spec default URI /p3/serviceValidate).
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

 http://localhost:8080/cas-management. (log in as casadmin:VerySecure)

Authentication Users
--------------------
The following users are configured in deployerConfigContext.xml for authentication:

Username | Password    | Remark
-------- | --------    | ------
casuser  | Mellon      | Normal user with NO access to /cas-management webapplication
casadmin | VerySecure  | Admin user with access to /cas-management webapplication


Warning
-------
*DO NOT USE THIS PROJECT AS PART OF ANY PRODUCTION BUILD*.
Instead, use a separate Java application server (Tomcat, JBoss, etc), properly secured,
and build a securely configured CAS server bundle to be deployed into that app server.

Keystore files
--------------
Apereo CAS relies on HTTPS. Therefore, the maven build creates a SSL Certificate if not existend and
stores it in a Java keystore file used by the embedded Jetty server, so the underlying Java implementation trusts this
self-signed certificate.

 * The JKS and PEM files are available in cas-server-overlay/target/jetty-ssl.keystore. (Useful for testing external CAS clients as well)

CAS URLs
--------
* Login: https://localhost:8443/cas/login
* Services Management: https://localhost:8443/cas/services/manage.html
* CAS 2.0 Service Validation: https://localhost:8443/cas/serviceValidate

More Information
----------------
 * [APEREO CAS](https://www.apereo.org/projects/cas)
 * [APEREO CAS on GitHub](https://github.com/jasig/cas)


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



