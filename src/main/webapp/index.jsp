<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
  <head>
    <title>DataONE Member Node Web Service Test</title>
    <style type="text/css">
      div { padding-left: 30px; padding-right: 50px; }
      body { font-family: serif; margin: 0px; }
      div.padded { margin-top: 30px; }
      div.indented { margin-left: 3%; margin-right: 20%;  }
      </style>
    </head>
  <body>
    <%
       String version = (String)request.getAttribute("d1_version");
       String revision = (String)request.getAttribute("d1_revision");
       String buildTime = (String)request.getAttribute("d1_buildTime");
       String sourceBranch = (String)request.getAttribute("d1_sourceBranch");
       %>
    <div style="background: #ccc; padding: 30px;">
      <a style ="font-weight: bold; font-size: larger;">DataONE Member Node Web
         Services Tester</a>
      <table>
        <tr>
          <td>Test Version / Revision / Build Date:</td>
          <td><%=version%> / <%=revision%> / <%=buildTime%></td>
        </tr>
        <tr>
          <td align="right">Source Branch:</td>
          <td><%=sourceBranch%></td>
        </tr>
      </table>
    </div>
    <form action="./mntester_" method="get">
      <div class="padded" style="font-weight: bold;">
        <table>
          <tr>
            <td>Member Node BaseURL:</td>
            <td>
              <input title="URL of Web Service" size="80" name="mNodeUrl" type="text" />
            </td>
          </tr>
          <tr>
            <td align="right">API Version:</td>
            <td>
              <select multiple title="API Version" size="2" name="selectedVersion">
                <option selected value="V1">v1</option>
                <option value="V2">v2</option>
              </select>
            </td>
          </tr>
          <tr>
            <td align="right">API(s) to test:</td>
            <td>
              <select multiple title="APIs to Test" size="6" name="selectedAPIs">
                <option value="MNCore">Tier 1: MNCore</option>
                <option value="MNRead">Tier 1: MNRead</option>
                <option value="MNAuthentication">Tier 1: Authentication</option>
                <option value="MNAuthorization">Tier 2: MNAuthorization</option>
                <option value="MNStorage">Tier 3: MNStorage</option>
                <option value="MNReplication">Tier 4: MNRepliation</option>
              </select>
            </td>
          </tr>
          <tr><td colspan=2><hr></hr></td></tr>
          <tr>
            <td align="right">Advanced:</td>
            <td style="font-weight: normal; font-size: 90%">
              Test-object series suffix** 
              <input title="Test Object Series Suffix" size="5" name="testObjectSeries" type="text" />
            </td>
          </tr>
          <tr><td colspan=2><hr></hr></td></tr>
          <tr> 
            <td></td>
            <td align="right">
              <input type="submit" alt="Submit" value="Submit"/>
            </td>
          </tr>
        </table>
      </div>
    </form>
    <div class="indented">
      <dl>
        <dt>Usage:</dt>
        <dd>to test basic Member Node (MN) API service implementations.<br></br></dd>
        <dt>Limitations:</dt>
        <dd>It cannot test every behavior required by MNs, particularly:
          <ul>
            <li>behavior that requires CN interaction (e.g. MN.getReplica)</li>
            <li>behavior that requires MN interaction (e.g. MN.replicate)</li>
            <li>method implementations that limit access by client subject</li>
          </ul>
        </dd>
        <dt>Organization:</dt>
        <dd>Tests are organized by API, with one or more test for each method in
          the API.  Tests cover not only expected "successful" behavior, but also
          that the proper exceptions are thrown as appropriate.<br></br></dd>
        <dt>Interpreting Results:</dt>
        <dd>Each test attempts to provide:
          <ul>
            <li>a meaningful name</li>
            <li>a descriptive failure message containing the last URL called</li>
            <li>a stack trace as a starting point for further introspection on the test</li>
          </ul>
        </dd>
      </dl>
    </div>
<!--       The Web Tester This service was designed to offer member node developers -->
<!--       an independent way to test their Member Node API implementations prior  -->
<!--       to the more focused and thorough testing done in partnership with DataONE  -->
<!--       team members in order to register the member node into the DataONE network. -->
<!--       Accordingly, the scope of tests is limited to those that can be run without  -->
<!--       interaction with other member or coordinating nodes, mostly looking to check  -->
<!--       the correctness of responses for all of the given API call.   -->
<!--     </div> -->
<!--     <div class="padded"> Tests are organized by API Tier level, and further organized -->
<!--       to run slower tests last within each tier.  For each API method, one or more  -->
<!--       test is run to cover common expected situations, even checking for proper  -->
<!--       exceptions as appropriate. Foreach Tier, a summary result is given (pass / fail). -->
<!--       Each test case also attempts to provide a descriptive exception message, but  -->
<!--       not every circumstance can be anticipated, so the stack trace is provided  -->
<!--       for further inspection.  -->
 
 
 
 
<!--       <div class="padded" style="font-size:smaller;">* <i>not including version path segment.   -->
<!-- 	      (e.g. </i> <b>"http://my.server.org/mn"</b> <i> rather than </i> <b>"http://my.server.org/mn/v1"</b> <i>)</i>  -->
<!-- 	</div> -->
    <div class="padded" style="font-size:90%;">** <i>Tier 2 Member Nodes need
     to have content owned by specific test subjects pre-loaded in order to pass 
     some Tier 2 tests. The series designator is the number or letter added as a suffix
     to the end of the testObject base name, put there to allow the set of test
     objects to evolve over time, or to supersede defective testObjects.  
     For example, if you created a 'TierTesting:testObject:Public_Read.1', you 
     need to set the value in the input field to "1" so the tester looks for that 
     object and its cohort. 
     Tier 3 and higher nodes do not need to specify a suffix, as the WebTester will 
     create the test objects it needs for Tier 2 tests.
    </div>
    <p></p>
    <p></p>
  </body>
</html>
