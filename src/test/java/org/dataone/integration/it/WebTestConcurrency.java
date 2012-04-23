/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

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
