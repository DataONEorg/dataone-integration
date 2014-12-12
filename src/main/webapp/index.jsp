<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
  <head>
    <title>DataONE Member Node Web Service Test</title>
    <style type="text/css">
      div { padding-left: 30px; padding-right: 50px; }
      body { font-family: serif; margin: 0px; }
      div.padded { margin-top: 30px; }
      </style>
    </head>
  <body>
    <%
       String version = (String)request.getAttribute("d1_version");
       String revision = (String)request.getAttribute("d1_revision");
       String buildTime = (String)request.getAttribute("d1_buildTime");
       String sourceBranch = (String)request.getAttribute("d1_sourceBranch");
       %>
    <div style="background: #ccc; padding: 30px; font-weight: bold;">DataONE Member Node Web
      Service Tests:</div>
    <div class="padded">
      <table>
	<tr>
	  <td>Test Version:</td>
	  <td><%=version%></td>
	  </tr>
	<tr>
	  <td>Source Branch:</td>
	  <td><%=sourceBranch%></td>
	  </tr>
	<tr>
	  <td>Test Revision:</td>
	  <td><%=revision%></td>
	  </tr>
	<tr>
	  <td>Build Time:</td>
	  <td><%=buildTime%></td>
	  </tr>
	</table>
      </div>
    <div class="padded">This service was designed to offer member node developers
      an independent way to test their Member Node API implementations prior 
      to the more focused and thorough testing done in partnership with DataONE 
      team members in order to register the member node into the DataONE network.
      Accordingly, the scope of tests is limited to those that can be run without 
      interaction with other member or coordinating nodes, mostly looking to check 
      the correctness of responses for all of the given API call.  
    </div>
    <div class="padded"> Tests are organized by API Tier level, and further organized
      to run slower tests last within each tier.  For each API method, one or more 
      test is run to cover common expected situations, even checking for proper 
      exceptions as appropriate. Foreach Tier, a summary result is given (pass / fail).
      Each test case also attempts to provide a descriptive exception message, but 
      not every circumstance can be anticipated, so the stack trace is provided 
      for further inspection. 
    </div>
    <form action="./mntester_" method="get">
      <div class="padded" style="font-weight: bold;">
	<table>
	  <tr>
	    <td>URL of DataONE Member Node Web Service*:</td>
	    <td>
	      <input title="URL of Web Service" size="80" name="mNodeUrl" type="text" />
	    </td>
	  </tr>
	  <tr>
        <td align="right">Tier-2 Node Test Object series suffix override** <em>(optional)</em>:</td>
        <td>
          <input title="Test Object Series Suffix" size="5" name="testObjectSeries" type="text" />
        </td>
      </tr>
	  <tr>
	    <td align="right">Tier(s) to test:</td>
	    <td>
	      <select multiple title="Tier Levels to Test" size="5" name="selectedTiers">
                <option selected value="Tier 0">Tier 0</option>
                <option>Tier 1</option>
                <option>Tier 2</option>
                <option>Tier 3</option>
                <option>Tier 4</option>
              </select>
	    </td>
	  </tr>
	  <tr>
	    <td/>
	    <td>
	      <input type="submit" alt="Submit" value="Submit"/>
	    </td>
	  </tr>
	</table>
      </div>
      <div class="padded" style="font-size:smaller;">* <i>not including version path segment.  
	      (e.g. </i> <b>"http://my.server.org/mn"</b> <i> rather than </i> <b>"http://my.server.org/mn/v1"</b> <i>)</i> 
	</div>
	<div class="padded" style="font-size:smaller;">** <i>Tier 2 Member Nodes need
	to have content owned by specific test subjects pre-loaded in order to pass 
	Tier 2 tests. The series designator is the number or letter added as a suffix
	to the end of the testObject base name, put there to allow the set of test
	objects to evolve over time, or to supplant defective testObjects.  
	For example, if you created a 'TierTesting:testObject:Public_Read.1', you 
	need to set the value in the input field to "1" so the tester looks for that 
	object and its cohort. 
	Tier 3 and higher nodes do not need to specify a suffix, as the WebTester will 
	create the test objects it needs for Tier 2 tests.
	</div>
      </form>
    </body>
</html>
