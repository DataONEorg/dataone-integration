package org.dataone.integration.it.testImplementations;

import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.service.types.v1.Node;

/**
 * A version-specific subclass of IsAuthorizedAuthorizationTestImplementations
 * to allow the version needed to instantiate the CommonCallAdapter to come from
 * the test class.
 * @author rnahf
 *
 */
public abstract class V1IsAuthorizedAuthorizationTestImpl extends
        IsAuthorizedAuthorizationTestImplementations
{

    @Override
    protected CommonCallAdapter instantiateD1Node(String subjectLabel, Node node)
    {
       return new CommonCallAdapter(getSession(subjectLabel), node, "v1");
    }

}
