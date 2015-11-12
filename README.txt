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
The d1_integration package is the default home for all integration tests for DataONE, 
as well as deployable testing apparatus for running certain integration test from the web
(the WebTester).  As a result, both unit tests for the testing apparatus and integration 
tests are part of this maven project.  

This package supports test execution from 5 main environments:
    a) Eclipse
    b) the commandline (via mvn)
    c) from our continuous integration server, Jenkins
    d) from a web browser (via deployment of the MNWebTester .war)
         - the default configuration for web tests only runs a subset 
           of tests ("MNodeTier" tests), but theoretically can run others.
    e) running the WebTester locally using an embedded Jetty server.

It currently supports execution of java-based tests and plans to support running
the python tests in the future.  Except when running via Eclipse, the surefire maven 
plugin is used, producing xml reports with execution details. For web tests, the
html output is produced by custom code listening in on a test run.
  

Usage:
------
Usage for this package follows the Maven WAR packaging lifecycle.  The following
goals do useful things:

 * mvn test      - used to run the unit tests
 * mvn package   - used to build the MNWebTester WAR file.
 * mvn verify    - used to run the integration tests
 
Java integration testing options are described fully at:
        http://maven.apache.org/plugins/maven-failsafe-plugin/examples/single-test.html

Examples: 
 running one integration test:             mvn -Dit.test={pathlessTestName} verify
 running multiple tests:                   mvn -Dit.test=*Core* verify
 
However: specifying methods within the class is supposed to work, 
    but doesn't in practice for reasons unknown.  For example the following
    will NOT work:
    
    running one method in integration test:   mvn -Dit.test={pathlessTestName}#{method} verify
    running subset of methods in test:        mvn -Dit.test=someTest#*get*


Deploying MNWebTester
---------------------
To deploy a new MNWebTester, do the following:
1. commit changes to svn
2. perform 'svn update'              to bring the revision number up-to-date on you local copy
3. optional:  perform 'mvn clean'    to remove old builds
4. perform 'mvn package'             to build the war file
5. scp target/MNWebTester{_some_version_and_revision}.war mncheck.test.dataone.org:
6. ssh to mncheck.test.dataone.org
7. sudo cp MNWebTester{...}.war /var/lib/tomcat6/webapps

Note: The pom contains some ant commands that don't work in the shell-less environment 
that Hudson uses when it runs mvn commands, so auto deploy of the MNWebTester doesn't
currently work on Hudson.



Running the WebTester Locally
-----------------------------
In addition to running the WebTester from mncheck.test.dataone.org, those who want
more control can run the WebTester locally using its embedded Jetty web server.  All
that is needed is a Java runtime, Subversion and Maven(3) installed, and a browser.
You will not need to work with java code.

This option lets you start and stop the server and cancel jobs, but gives you the
same functionality and UI as the WebTester DataONE maintains on mncheck.test.dataone.org.  
You will also have access to the log messages the webapp produces.

For *NIX OS's

1. install subversion if necessary

2. install maven if necessary

3. install a Java JDK if necessary (compiling the tests via maven requires a JDK)

4. checkout this project, if necessary, either the development branch (/trunk) 
   or a released branch (/branches/D1_INTEGRATION_v[X].[Y]
   For example:
      svn co https://repository.dataone.org/software/cicore/trunk/d1_integration
      svn co https://repository.dataone.org/software/cicore/branches/D1_INTEGRATION_v1.1
      
5. from the checked-out d1_integration top-level directory, build the .war file (the web app)
      $ mvn package
      
6. run the MNWebTester:
      $ java -jar target/MNWebTester_<some version and build number>.war
      
      # this starts an embedded jetty server that will be listening on port 6680 
      
7. run / connect to the MNWebTester by pointing your browser to

     http://localhost:6680
     
     Note: If you get an exception that tells you there was a problem compiling with javac
     and a full JDK is required instead of a JRE, you may need to change your java config
     to make sure the JDK is enabled and to disable JREs, or do step 6 with this parameter:
     
     java -jar -Dorg.apache.jasper.compiler.disablejsr199=true target/MNWebTester_<some version and build number>.war
     
8. from the browser page, enter the url of the Member Node you wish to test, and
   choose the tests you wish to run.


You can stop the server with ^C or through a telnet command to a listener on the adjacent port:

      $ telnet localhost 6681
      <tap return key again after issuing the command above>


Running the WebTester tests locally
-----------------------------------
For those more comfortable running maven tests from the command line, you can also
check out the d1_integration project and run the tests through the command line.
Note: for v1.x d1_integration checkouts, tests are organized by Tier, but in v2 
we reorganized the tests into API names, for greater flexibility.

For example: 
   
   mvn -Dcontext.mn.baseurl=https://mn.foo.org/mn -Dit.test=MNodeTier1IT verify
   mvn -Dcontext.mn.baseurl=https://mn.foo.org/mn -Dit.test=MNodeTier* verify
   
   or for v2:
   mvn -Dcontext.mn.baseurl=https://mn.foo.org/mn -Dit.test=MNCore* verify
   mvn -Dcontext.mn.baseurl=https://mn.foo.org/mn -Dit.test=MNRead* verify
   
In the examples above, the baseurl and test sets are specified with -D options,
and both are necessary.  The verify is the life-cycle goal maven is given that
will initiate building, unit testing, packaging, and lastly run the integration 
tests matching the pattern you specify.

All of the WebTester tests are organized in the project under:
  
  v1: src/test/java/org/dataone/integration/it/MNodeTier*
  v2: src/test/java/org/dataone/integration/it/apiTests/MN*

Additionally, Tier2 and above tests make use of a set of client certificates when making 
api calls.  These need to be downloaded separately and installed on the machine
from where the tests are being run.  Please contact support@dataone.org to request
these certificates.

the property (and default value) that controls where the libclient will look for these is:

  d1.test.cert.location=/etc/dataone/client/testClientCerts/

Installation involves securing that path so that it is private to the user running
the tests, THEN extracting the certificates into the directory. The following shows
the starting tar file and the extracted directory.  Note that this entire directory
is secured by removing any permissions for group or other.  (chmod 700 .)

 0 drwx------   6 me  staff    204 May 21 11:49 .
 0 drwxr-xr-x  68 me  staff   2312 May 21 10:06 ..
 8 lrwxr-xr-x   1 me  staff     24 May 21 11:49 testClientCerts -> testClientCerts-20120518
 0 drwxr-xr-x  12 me  staff    408 May 18 23:09 testClientCerts-20120518
48 -rw-r--r--   1 me  staff  23360 May 21 11:48 testClientCerts-20120518.tgz

For more advanced local setups, please contact the dataone core team for advice
and additional context properties.



Notes for further development:
------------------------------
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