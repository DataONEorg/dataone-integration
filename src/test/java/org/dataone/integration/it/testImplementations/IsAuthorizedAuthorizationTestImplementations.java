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

package org.dataone.integration.it.testImplementations;

import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Permission;

public abstract class IsAuthorizedAuthorizationTestImplementations extends AbstractAuthorizationTestImplementations {


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
            log.info("Running isAuthorized() on " + cca.getLatestRequestUrl() 
                    + " with pid " + pid.getValue() + " to check for permission: " 
                    + permission.name() + " ...");
            boolean booleanOutcome = cca.isAuthorized(null, pid, permission);
            outcome = booleanOutcome ? "true" : "false";
        }
        catch (BaseException e) {
            outcome = e.getClass().getSimpleName();
            log.error("isAuthorized() returned exception: " + e.getClass().getSimpleName() 
                    + e.getDetail_code() + " : " + e.getMessage() + " : " 
                    + e.getDescription(), e);
        } catch (ClientSideException e) {
            outcome = e.getClass().getSimpleName();
            log.error("isAuthorized() returned exception: " + e.getClass().getSimpleName() 
                    + " : " + e.getMessage());
        }
        return outcome;
    }
}
