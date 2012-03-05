<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
  <head>
    <title>DataONE Member Node Web Service Test</title>
    <style type="text/css">
      div { padding-left: 10px; padding-right: 10px; }
      body { font-family: serif; margin: 0px; }
      div.padded { margin-top: 20px; }
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
    <div class="padded">This service runs basic tests to check the correctness of a DataONE
      Member Node Web Services interface. Tier 4 methods are currently excluded, as they
      require interaction with 2 other nodes.  (Tests for Tier 4 methods will necessitate
      another strategy, and for now, please contact the DataONE developers team for advice.)
      </div>
    <div class="padded">In this test service, we provide one or more test for each method
          to cover common expected situations, and a summary result by Tier is given (pass / fail).
          Each test case attempts to provide a descriptive exception message, but not every
          circumstance can be anticipated, so the stack trace is provided for further inspection. 
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
	    <td align="right">Maximum Tier to test:</td>
	    <td>
	      <select title="Maximum Tier Level to Test" size="1" name="maxTier">
                <option selected value="Tier4">Tier 4</option>
                <option>Tier 3</option>
                <option>Tier 2</option>
                <option>Tier 1</option>
                <option>Tier 0</option>
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
      </form>
    </body>
</html>
