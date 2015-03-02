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

package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import org.dataone.integration.TestIterationEndingException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;

@WebTestDescription("TestCase class annotation")
public class MockV1ITCase {


    @Test
    public void testSuccess()
    {
        assertTrue(true);
    }


//	@Test
//	public void testFailure()
//	{
//		assertTrue(1==2);
//	}


    @Ignore
    @Test
    public void testIgnore() {
        ;
    }


//	@Test
//	public void testError() {
//		// should throw a numerical exception of some type
//		float f = 12 / 0;
//		f++;
//	}

       @Test
        public void testWarning() throws TestIterationEndingException {
            // should throw a numerical exception of some type
            throw new TestIterationEndingException("");
        }

    @Test
    public void testAssumptionViolatedException() {
        throw new AssumptionViolatedException("Could not set up the test properly.");
    }
}
