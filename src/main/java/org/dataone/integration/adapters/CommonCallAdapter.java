package org.dataone.integration.adapters;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Date;

import org.dataone.client.D1Node;
import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.cn.v1.CNAuthorization;
import org.dataone.service.cn.v1.CNCore;
import org.dataone.service.cn.v2.CNRead;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.mn.tier1.v1.MNCore;
import org.dataone.service.mn.tier1.v1.MNRead;
import org.dataone.service.mn.tier2.v1.MNAuthorization;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;


/**
 *  CommonCallAdapter is a class built to allow certain API tests to be reused 
 *  for API methods that have the same structure and behavior between CN and MN
 *  and V1 and V2 implementations.  Instead of instantiating a v1.MNode, the test
 *  instantiates an instance of this class, and calls the corresponding method.
 *  
 *  The version and NodeType are specified during instantiation.  
 *  
 *  Methods from these classes differ from methods from the MNode or CNode in 
 *  that they also throw a ClientSideException.
 * @author rnahf
 *
 */
public class CommonCallAdapter implements D1Node {

    protected Node node;
    protected String version;
    protected MultipartRestClient mrc;

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
        throw new ClientSideException("Ping failed. " + node.getType() + " of version " + version);
    }

    
    // TODO: test that TypeMarshaller.convertTypeFromType works for Log class(es)
    public Log getLogRecords(Session session, Date fromDate, Date toDate, String event, String pidFilter,
            Integer start, Integer count) throws ClientSideException, InvalidRequest, InvalidToken, NotAuthorized,
            NotImplemented, ServiceFailure, InsufficientResources {
        
        try {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNCore mnCore = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                // TODO use deprecated call with Session? or no?
                org.dataone.service.types.v1.Log log = mnCore.getLogRecords(session, fromDate, toDate,
                        Event.convert(event), pidFilter, start, count);
//                return TypeMarshaller.convertTypeFromType(log, Log.class);
                return convertV1Log(log);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNCore mnCore = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc, URI.create(this.node.getBaseURL()));
                Log log = mnCore.getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                return log;
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                CNCore cnCore = D1NodeFactory.buildNode(org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                org.dataone.service.types.v1.Log log = cnCore.getLogRecords(session, fromDate, toDate,
                        Event.convert(event), pidFilter, start, count);
//                return TypeMarshaller.convertTypeFromType(log, Log.class);
                return convertV1Log(log);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc, URI.create(this.node.getBaseURL()));
                Log log = cnCore.getLogRecords(session, fromDate, toDate, event, pidFilter, start, count);
                return log;
            }
        }
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | JiBXException | IOException e) {
            
            throw new ClientSideException("Unable to convert a v1.Log to a v2.Log", e);
        }
        finally {}
        
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }

    public Node getCapabilities() throws ClientSideException, NotImplemented, ServiceFailure {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNCore mnCore = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnCore.getCapabilities();
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNCore mnCore = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNCore.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnCore.getCapabilities();
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            throw new UnsupportedOperationException("CN nodes to not support a getCapabilities call.");
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
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

    public InputStream get(Session session, Identifier pid) throws ClientSideException, InvalidToken, NotAuthorized,
            NotImplemented, ServiceFailure, NotFound, InsufficientResources {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNRead mnRead = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.get(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnRead.get(session, pid);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.get(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.get(session, pid);
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }

    public org.dataone.service.types.v2.SystemMetadata getSystemMetadata(Session session,
            Identifier pid) throws ClientSideException, InvalidToken, NotAuthorized,
            NotImplemented, ServiceFailure, NotFound {
        try {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                SystemMetadata systemMetadata = mnRead.getSystemMetadata(session, pid);
                return TypeMarshaller.convertTypeFromType(systemMetadata,
                        org.dataone.service.types.v2.SystemMetadata.class);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.getSystemMetadata(session, pid);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                SystemMetadata systemMetadata = cnRead.getSystemMetadata(session, pid);
                return TypeMarshaller.convertTypeFromType(systemMetadata,
                        org.dataone.service.types.v2.SystemMetadata.class);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.getSystemMetadata(session, pid);
            }
        }
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | JiBXException | IOException e) 
        {
            throw new ClientSideException("Unable to convert SystemMetadata from type to type");
        }
        throw new ClientSideException("Unable to create node of type " + node.getType()
                + " of version " + version);
    }
    
    public DescribeResponse describe(Session session, Identifier pid)
            throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNRead mnRead = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.describe(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnRead.describe(session, pid);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.describe(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.describe(session, pid);
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }
    
    public Checksum getChecksum(Session session, Identifier pid, String checksumAlgorithm)
            throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented,
            ServiceFailure, NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNRead mnRead = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.getChecksum(session, pid, checksumAlgorithm);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnRead.getChecksum(session, pid, checksumAlgorithm);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.getChecksum(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.getChecksum(session, pid);
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }

    /**
     * This method is compatible with v1 and v2 listObjects, but does not have
     * the Identifier parameter that is added to the v2 method.
     * @param session
     * @param fromDate
     * @param toDate
     * @param formatID
     * @param replicaStatus
     * @param start
     * @param count
     * @return
     * @throws InvalidRequest
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @throws ClientSideException
     */
    public ObjectList listObjects(Session session, Date fromDate, Date toDate, ObjectFormatIdentifier formatID,
            Boolean replicaStatus, Integer start, Integer count) throws InvalidRequest,
            InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClientSideException {
        return listObjects(session, fromDate, toDate, formatID, null /* identifier */, 
                replicaStatus, start, count);
    }
    
    
    public ObjectList listObjects(Session session, Date fromDate, Date toDate, ObjectFormatIdentifier formatID,
            Identifier identifier, Boolean replicaStatus, Integer start, Integer count) throws InvalidRequest,
            InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                if (identifier == null) {
                    MNRead mnRead = D1NodeFactory.buildNode(org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                    return mnRead.listObjects(session, fromDate, toDate, formatID, replicaStatus, start, count);
                } else {
                    throw new InvalidRequest("0000", "The identifier field can only be null for v1.listObject calls");
                }
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier1.v2.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v2.MNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnRead.listObjects(session, fromDate, toDate, formatID, identifier, replicaStatus, start, count);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                if (identifier == null) {
                    org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                    return cnRead.listObjects(session, fromDate, toDate, formatID, replicaStatus, start, count);
                } else {
                    throw new InvalidRequest("0000", "The identifier field can only be null for v1.listObjects calls");
                }
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnRead.listObjects(session, fromDate, toDate, formatID, identifier, replicaStatus, start, count);
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    }
    
    public boolean isAuthorized(Session session, Identifier id, Permission permission) 
    throws NotAuthorized, ServiceFailure, NotImplemented, InvalidToken, NotFound, 
    InvalidRequest, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNAuthorization mNode = D1NodeFactory.buildNode(MNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mNode.isAuthorized(session, id, permission);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.mn.tier2.v2.MNAuthorization mnAuth = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier2.v2.MNAuthorization.class, this.mrc, URI.create(this.node.getBaseURL()));
                return mnAuth.isAuthorized(session, id, permission);
            }
        } else if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                CNAuthorization cnAuth = D1NodeFactory.buildNode(
                        CNAuthorization.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnAuth.isAuthorized(session, id, permission);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.cn.v2.CNAuthorization cnAuth = D1NodeFactory.buildNode(org.dataone.service.cn.v2.CNAuthorization.class, this.mrc, URI.create(this.node.getBaseURL()));
                return cnAuth.isAuthorized(session, id, permission);
            }
        }
        throw new ClientSideException("Unable to create node of type " + node.getType() + " of version " + version);
    
    }
    
    private Log convertV1Log(org.dataone.service.types.v1.Log v1Log) 
    throws InstantiationException, IllegalAccessException, InvocationTargetException, JiBXException, IOException 
    {
        Log v2log = new Log();
        if (v1Log.getLogEntryList() != null && v1Log.getLogEntryList().size() > 0)
            for(org.dataone.service.types.v1.LogEntry v1entry : v1Log.getLogEntryList()) {
                LogEntry v2LogEntry = TypeMarshaller.convertTypeFromType(v1entry, LogEntry.class);
                v2log.addLogEntry(v2LogEntry);
            }
        return v2log;
    }

}
