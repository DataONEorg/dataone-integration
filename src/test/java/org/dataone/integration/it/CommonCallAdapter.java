package org.dataone.integration.it;

import java.net.URI;
import java.util.Date;

import org.dataone.client.D1Node;
import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.Log;

public class CommonCallAdapter implements D1Node {

    Node node;
    String version;
    MultipartRestClient mrc;

    public CommonCallAdapter(MultipartRestClient mrc, Node node, String version) {
        this.mrc = mrc;
        this.node = node;
        this.version = version;
    }

    public Date ping() throws NotImplemented, ServiceFailure, InsufficientResources, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                return D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNCore.class, 
                    this.mrc, URI.create(this.node.getBaseURL())).ping();
            } else if (this.version.toLowerCase().equals("v2")) {
                return D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL())).ping();
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                return D1NodeFactory.buildNode(org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL())).ping();
            } else if (this.version.toLowerCase().equals("v2")) {
                return D1NodeFactory.buildNode(org.dataone.service.cn.v2.CNCore.class,
                        this.mrc, URI.create(this.node.getBaseURL())).ping();
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }

    public Log getLogRecords(Session session, Date fromDate, Date toDate, String event, String pidFilter,
            Integer start, Integer count) throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
            ServiceFailure {
        // TODO Auto-generated
        return null;
    }

    public Node getCapabilities() throws NotImplemented, ServiceFailure {
        return null;
    }

    @Override
    public String getNodeBaseServiceUrl() {
        //  MNodes and CNodes usually override this 
        // method by adding the verion path segment, so we will too.
        return node.getBaseURL() + "/" + this.version.toLowerCase();
    }

    @Override
    public NodeReference getNodeId() {
        return this.node.getIdentifier();
    }

    @Override
    public void setNodeId(NodeReference nodeId) {
        ;
    }

    @Override
    public void setNodeType(NodeType nodeType) {
        ;
    }

    @Override
    public NodeType getNodeType() {
        // TODO Auto-generated method stub
        return this.node.getType();
    }

    @Override
    public String getLatestRequestUrl() {
        return mrc.getLatestRequestUrl();
    }

}
