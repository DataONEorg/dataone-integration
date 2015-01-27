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

import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Node;
import nu.xom.Serializer;

/**
 * expose the writeChild(Node n) method to allow serializing the div's in
 * the WebTester output independently.
 *
 * Needs to be used with caution, because it bypasses the logic of the document
 * The user needs to call flush() to put the output on the passed in outputstream
 *
 * @author rnahf
 *
 */
public class StreamableSerializer extends Serializer {


    public StreamableSerializer(OutputStream arg0) {
        super(arg0);
    }


    public void writeChild(Node node) throws IOException {
        super.writeChild(node);
    }

}
