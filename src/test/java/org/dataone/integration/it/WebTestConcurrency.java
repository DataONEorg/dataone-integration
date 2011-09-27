package org.dataone.integration.it;

import java.util.Iterator;
import java.util.UUID;

import org.dataone.service.types.v1.Node;
import org.junit.Test;


public class WebTestConcurrency extends ContextAwareTestCaseDataone {

	 private  String currentUrl;
	
	@Override
	protected String getTestDescription() {
		return "Test Case to diagnose concurrency issues related to running" +
				"junit by servlet.";
	}
	
	
	@Test
	public void testA() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	@Test
	public void testB() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	@Test
	public void testC() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	@Test
	public void testD() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	@Test
	public void testE() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	@Test
	public void testF() throws InterruptedException {
		Thread.sleep(5000);
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			long threadid = Thread.currentThread().getId();
			handleFail(currentUrl,"threadID: " + threadid + "  UUID: " + UUID.randomUUID());
		}
	}
	
	
}
