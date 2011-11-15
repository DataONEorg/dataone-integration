package org.dataone.integration.webTest;

import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestRunnerIndexServlet extends HttpServlet {
	
	private static final String D1_BUILD_TIME = "D1-Build-TimeStamp";
	private static final String D1_REVISION = "D1-SCM-Revision";
	private static final String D1_VERSION = "D1-version";
	private static final String D1_SOURCE_BRANCH = "D1-SCM-Branch";
	
	private DateFormat format = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	
    	Manifest manifest = new Manifest(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
    	
    	setBuildContextInfo(request, manifest.getMainAttributes());
    	
    	RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.jsp");
    	rd.forward(request, response);
    }


	private void setBuildContextInfo(HttpServletRequest request,
			Attributes attributes) {
		String version = attributes.getValue(D1_VERSION);
    	if (version == null) {
    		version = "";
    	}
    	request.setAttribute("d1_version", version);
    	
    	String revision = attributes.getValue(D1_REVISION);
    	if (revision == null) {
    		revision = "";
    	}
    	request.setAttribute("d1_revision", revision);
    	
    	String sourceBranch = attributes.getValue(D1_SOURCE_BRANCH);
    	if (sourceBranch == null) {
    		sourceBranch = "";
    	}
    	request.setAttribute("d1_sourceBranch", sourceBranch);

    	String buildTime = attributes.getValue(D1_BUILD_TIME);
    	if (buildTime != null) {
    		buildTime = format.format(new Date(Long.parseLong(buildTime)));
    	} else {
    		buildTime = "";
    	}
    	request.setAttribute("d1_buildTime", buildTime);
	}


    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	this.doGet(request, response);
    }
	
}
