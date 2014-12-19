package org.dataone.integration;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Date;

import org.dataone.client.D1Node;
import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.cn.v1.CNCore;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.mn.tier1.v1.MNCore;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v2.Log;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;

public class CommonCallAdapter implements D1Node {

    private Node node;
    private String version;
    private MultipartRestClient mrc;

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
            ServiceFailure, ClientSideException, InstantiationException, IllegalAccessException,
            InvocationTargetException, JiBXException, IOException, InsufficientResources {

        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNCore mnCore = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                // TODO use deprecated call with Session? or no?
                org.dataone.service.types.v1.Log log = mnCore.getLogRecords(session, fromDate, toDate,
                        Event.convert(event), pidFilter, start, count);
                return TypeMarshaller.convertTypeFromType(log, Log.class);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNCore mnCore = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc, URI.create(this.node.getBaseURL()));
                Log log = mnCore.getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                return log;
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                CNCore mnCore = D1NodeFactory.buildNode(org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                org.dataone.service.types.v1.Log log = mnCore.getLogRecords(session, fromDate, toDate,
                        Event.convert(event), pidFilter, start, count);
                return TypeMarshaller.convertTypeFromType(log, Log.class);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNCore mnCore = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc, URI.create(this.node.getBaseURL()));
                Log log = mnCore.getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                return log;
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
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


	public DescribeResponse describe(Session session, Identifier arg1)
			throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
			NotFound {
		// TODO Auto-generated method stub
		return null;
	}


	public InputStream get(Session session, Identifier arg1) throws InvalidToken,
			NotAuthorized, NotImplemented, ServiceFailure, NotFound,
			InsufficientResources {
		// TODO Auto-generated method stub
		return null;
	}


	public Checksum getChecksum(Session session, Identifier arg1, String arg2)
			throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
			ServiceFailure, NotFound {
		// TODO Auto-generated method stub
		return null;
	}

	public InputStream getReplica(Session session, Identifier arg1)
			throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
			NotFound, InsufficientResources {
		// TODO Auto-generated method stub
		return null;
	}


	public SystemMetadata getSystemMetadata(Session session, Identifier arg1)
			throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
			NotFound {
		// TODO Auto-generated method stub
		return null;
	}


	public ObjectList listObjects(Session session, Date arg1, Date arg2,
			ObjectFormatIdentifier arg3, Boolean arg4, Integer arg5,
			Integer arg6) throws InvalidRequest, InvalidToken, NotAuthorized,
			NotImplemented, ServiceFailure {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Boolean isAuthorized(Session session, Identifier id, Permission permissionLevel) 
	throws NotAuthorized, NotFound, ServiceFailure, NotImplemented, InvalidRequest
	{
		// TODO Auto-generated method stub  (check the thrown exceptions - I guessed on those)
		return null;
	}
	
	public NodeList CNlistNodes() 
		throws ServiceFailure, NotImplemented
	{
		// TODO auto-generated method stub
		return null;
	}
}
