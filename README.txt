====
    This work was created by participants in the DataONE project, and is
    jointly copyrighted by participating institutions in DataONE. For
    more information on DataONE, see our web site at http://dataone.org.

      Copyright ${year}

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: README.txt 2498 2010-10-27 18:53:33Z jones $
====

DataONE Java Integration Testing
--------------------------------
The d1_integration package is the home for all integration tests for DataONE, as well
as deployable testing apparatus for running certain integration test from the web. As a 
result, both unit tests for the testing apparatus and integration tests are part of 
this maven project.  

This package supports test execution from 4 main environments:
    a) Eclipse
    b) the commandline (via mvn)
    c) from our continuous integration server, Hudson
    d) from a web browser

It currently supports execution of java-based tests and plans to support running
the python tests soon.  Except when running via Eclipse, the surefire maven 
plugin is used, producing xml reports with execution details. For web tests, the
html output is produced by custom code listening in on a test run.

See LICENSE.txt for the details of distributing this software.  

Usage:
------
Usage for this package follows the Maven WAR packaging lifecycle.  In particular

Running all tests:
 * mvn test   - used to run the unit tests
 * mvn verify - used to run the integration tests
 * mvn install - used to install the WAR in your local maven repository

 * mvn tomcat:deploy - used to deploy the war to a local tomcat server (local
       configuration required)

Java integration testing options **
  described fully at:
        http://maven.apache.org/plugins/maven-failsafe-plugin/examples/single-test.html

Examples: 
 running one integration test:             mvn -Dit.test={pathlessTestName} verify
 running multiple tests:                   mvn -Dit.test=*Core* verify
 
 ** However: specifying methods within the class is supposed to work, 
 but doesn't in practice for reasons unknown.
    running one method in integration test:   mvn -Dit.test={pathlessTestName}#{method} verify
    running subset of methods in test:        mvn -Dit.test=someTest#*get*


Notes for Developers:
---------------------
Integration tests are defined as tests that require infrastructure beyond the current
package and it's dependencies.  This would include functional and use case tests, but
not the unit tests that typically must pass before committing the code.

* Regarding local tomcat deployment, see:
http://mojo.codehaus.org/tomcat-maven-plugin/deployment.html

* Regarding python integration
http://steveberczuk.blogspot.com/2009/12/continuous-integration-of-python-code.html
https://github.com/jacek99/maven-python-mojos
https://github.com/jacek99/maven-python-mojos/tree/master/maven-python-mojos/maven-python-distribute-plugin/


* Integration Testing strategies as related to maven and continuous integration 
(old proposals prior to failsafe plugin, I believe)
http://docs.codehaus.org/display/MAVENUSER/Maven+and+Integration+Testing
http://docs.codehaus.org/display/MAVEN/Testing+Strategies
http://docs.codehaus.org/display/MAVEN/best+practices+-+testing+strategies


http://garbuz.com/2010/07/31/maven-2-deploying-project-sources/

General TO DOs
--------------
1. tie pyunit testing into maven lifecycle.
  a). what about python dependency management?  
      http://nathanmarz.com/blog/introducing-nanny-a-really-simple-dependency-management-tool.html