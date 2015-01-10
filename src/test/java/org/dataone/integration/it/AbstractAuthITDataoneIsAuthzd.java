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

import org.dataone.client.D1Node;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;

public abstract class AbstractAuthITDataoneIsAuthzd extends AbstractAuthorizationITDataone {

    @Override
    protected  boolean runTest(Permission p) {
        return true;
    }

    @Override
    protected String getTestServiceEndpoint() {
        return "/isAuthorized/{ pid }?action={ permission }";
    }

    @Override
    protected String runAuthTest(CommonCallAdapter cca, Identifier pid, Permission permission)
    {
        String outcome = null;
        try {
            boolean booleanOutcome = cca.isAuthorized(null, pid, permission);
            outcome = booleanOutcome ? "true" : "false";
        }
        catch (BaseException e) {
            outcome = e.getClass().getSimpleName();
        }
        return outcome;
    }
}
