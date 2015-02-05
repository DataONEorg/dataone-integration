package org.dataone.integration.it.testImplementations;

import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;

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

    protected String version;
    
    protected void setApiVersion(String version) {
        this.version = version;
    }
    
    @Override
    protected CommonCallAdapter instantiateD1Node(String subjectLabel, Node node)
    {
       if(node.getType().equals(NodeType.MN)) {
           return new MNCallAdapter(getSession(subjectLabel), node, version);
       } else {
           return new CNCallAdapter(getSession(subjectLabel), node, version);
       } 
    }

}
