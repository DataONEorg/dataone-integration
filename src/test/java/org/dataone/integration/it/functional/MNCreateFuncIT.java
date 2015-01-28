package org.dataone.integration.it.functional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.util.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v1.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Test;

public class MNCreateFuncIT { // extends ContextAwareTestCaseDataone {

	public String getTestDescription() {
		return "try creating under a different name";
	}
	
	@Test
	public void testMetadataCreate() {
        Log log = LogFactory.getLog(MNCreateFuncIT.class);
        
        
        
        MNode mnSource = new MNode("http://dev.datadryad.org/mn");
        MNode mnTarget = D1Client.getMN("https://mn-demo-5.test.dataone.org/knb/d1/mn");

        SystemMetadata sysmeta;
        
		try {
			Identifier origPid = D1TypeBuilder.buildIdentifier("http://dx.doi.org/10.5061/dryad.12?ver=2011-08-02T16:00:05.530-0400");
			log.warn("Original PID: " + origPid.getValue());
			InputStream is = mnSource.get(origPid);
			byte[] ba = IOUtils.toByteArray(is);
			System.out.println(new String(ba));
			
			SystemMetadata smd = mnSource.getSystemMetadata(origPid);
			Identifier newPid = D1TypeBuilder.buildIdentifier("testDryadMD_newSchema");
			smd.setIdentifier(newPid);
			smd.setAuthoritativeMemberNode(D1TypeBuilder.buildNodeReference("urn:node:mnDemo5"));
			log.warn("formatId: " + smd.getFormatId().getValue());
			
			
			
	        Identifier identifier = mnTarget.create(smd.getIdentifier(),
	        		new ByteArrayInputStream(ba), smd);

	        log.debug("Created identifier " + identifier.getValue());
	        
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (BaseException e) {
			e.printStackTrace();
			
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
        
	}

}