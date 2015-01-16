package org.dataone.integration.adapters;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.mn.tier1.v2.MNRead;
import org.dataone.service.mn.tier2.v1.MNAuthorization;
import org.dataone.service.mn.tier3.v2.MNStorage;
import org.dataone.service.mn.tier4.v2.MNReplication;
import org.dataone.service.mn.v2.MNQuery;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.SystemMetadata;

/**
 * Subclass of {@link CommonCallAdapter} that can be used to call MNode API methods that
 * have similar structure and behavior across versions. Extends CommonCallAdapter
 * to allow tests to call methods sequentially from both with the same state.
 * </p><b>Warning:</b> Calls that are invalid (don't exist for a specific version, or don't apply to the node given) will throw a {@link ClientSideException}.
 */
public class MNCallAdapter extends CommonCallAdapter {

    public MNCallAdapter(MultipartRestClient mrc, Node node, String version) {
        super(mrc, node, version);
    }


    // MNRead:
    
    public boolean systemMetadataChanged(Session session, Identifier id, long serialVersion,
            Date dateSystemMetadataLastModified) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, InvalidRequest, ClientSideException {
        
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                MNAuthorization mnAuth = D1NodeFactory.buildNode(MNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnAuth.systemMetadataChanged(session, id, serialVersion,
                        dateSystemMetadataLastModified);
            } else if(this.version.toLowerCase().equals("v2")) {
                MNRead mnRead = D1NodeFactory.buildNode(MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.systemMetadataChanged(session, id, serialVersion,
                        dateSystemMetadataLastModified);
            }
        }
        throw new ClientSideException("Call to systemMetaDataChanged failed. " + node.getType()
                + " of version " + version);
    }

    public boolean synchronizationFailed(Session session, SynchronizationFailed message)
            throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClientSideException {

        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier1.v1.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.synchronizationFailed(session, message);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNRead mnRead = D1NodeFactory.buildNode(MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.synchronizationFailed(session, message);
            }
        }
        throw new ClientSideException("Call to synchronizationFailed failed. " + node.getType()
                + " of version " + version);
    }

    public InputStream getReplica(Session session, Identifier pid) throws InvalidToken,
            NotAuthorized, NotImplemented, ServiceFailure, NotFound, InsufficientResources, ClientSideException {

        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier1.v1.MNRead mnRead = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier1.v1.MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.getReplica(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNRead mnRead = D1NodeFactory.buildNode(MNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnRead.getReplica(session, pid);
            }
        }
        throw new ClientSideException("Call to getReplica failed. " + node.getType()
                + " of version " + version);
    }

    // MNStorage:
    
    public Identifier create(Session session, Identifier pid, InputStream object, 
            SystemMetadata sysmeta) 
        throws IdentifierNotUnique, InsufficientResources, InvalidRequest, InvalidSystemMetadata, 
            InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, UnsupportedType, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier3.v1.MNStorage mnStorage = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier3.v1.MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.create(session, pid, object, sysmeta);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.create(session, pid, object, sysmeta);
            }
        }
        throw new ClientSideException("Call to create failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier update(Session session, Identifier pid, InputStream object,
            Identifier newPid, SystemMetadata sysmeta) throws IdentifierNotUnique,
            InsufficientResources, InvalidRequest, InvalidSystemMetadata, InvalidToken,
            NotAuthorized, NotImplemented, ServiceFailure, UnsupportedType, NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier3.v1.MNStorage mnStorage = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier3.v1.MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.update(session, pid, object, newPid, sysmeta);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.update(session, pid, object, newPid, sysmeta);
            }
        }
        throw new ClientSideException("Call to update failed. " + node.getType()
                + " of version " + version);
    }

    public boolean updateSystemMetadata(Session session, Identifier pid, SystemMetadata sysmeta)
            throws NotImplemented, NotAuthorized, NotFound, ServiceFailure, InvalidRequest,
            InvalidSystemMetadata, InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)
                && this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.updateSystemMetadata(session, pid, sysmeta);
        }
        throw new ClientSideException("Call to updateSystemMetadata failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier delete(Session session, Identifier id) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier3.v1.MNStorage mnStorage = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier3.v1.MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.delete(session, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.delete(session, id);
            }
        }
        throw new ClientSideException("Call to delete failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier archive(Session session, Identifier id) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier3.v1.MNStorage mnStorage = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier3.v1.MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.archive(session, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.archive(session, id);
            }
        }
        throw new ClientSideException("Call to archive failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier generateIdentifier(Session session, String scheme, String fragment)
            throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier3.v1.MNStorage mnStorage = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier3.v1.MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.generateIdentifier(session, scheme, fragment);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNStorage mnStorage = D1NodeFactory.buildNode(MNStorage.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnStorage.generateIdentifier(session, scheme, fragment);
            }
        }
        throw new ClientSideException("Call to create failed. " + node.getType()
                + " of version " + version);
    }

    public boolean replicate(Session session, SystemMetadata sysmeta, NodeReference sourceNode)
            throws NotImplemented, ServiceFailure, NotAuthorized, InvalidRequest, InvalidToken,
            InsufficientResources, UnsupportedType, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.tier4.v1.MNReplication mnReplication = D1NodeFactory.buildNode(
                        org.dataone.service.mn.tier4.v1.MNReplication.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnReplication.replicate(session, sysmeta, sourceNode);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNReplication mnReplication = D1NodeFactory.buildNode(MNReplication.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnReplication.replicate(session, sysmeta, sourceNode);
            }
        }
        throw new ClientSideException("Call to replicate failed. " + node.getType()
                + " of version " + version);
    }

    public InputStream query(Session session, String queryEngine, String query)
            throws InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented,
            NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.v1.MNQuery mnQuery = D1NodeFactory.buildNode(
                        org.dataone.service.mn.v1.MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.query(queryEngine, query);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNQuery mnQuery = D1NodeFactory.buildNode(MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.query(session, queryEngine, query);
            }
        }
        throw new ClientSideException("Call to query failed. " + node.getType()
                + " of version " + version);
    }
    
    public QueryEngineDescription getQueryEngineDescription(Session session, String queryEngine)
            throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.v1.MNQuery mnQuery = D1NodeFactory.buildNode(
                        org.dataone.service.mn.v1.MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.getQueryEngineDescription(queryEngine);
            } else if (this.version.toLowerCase().equals("v2")) {
                MNQuery mnQuery = D1NodeFactory.buildNode(MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.getQueryEngineDescription(session, queryEngine);
            }
        }
        throw new ClientSideException("Call to getQueryEngineDescription failed. " + node.getType()
                + " of version " + version);
    }


    public QueryEngineList listQueryEngines(Session session) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.MN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.mn.v1.MNQuery mnQuery = D1NodeFactory.buildNode(
                        org.dataone.service.mn.v1.MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.listQueryEngines();
            } else if (this.version.toLowerCase().equals("v2")) {
                MNQuery mnQuery = D1NodeFactory.buildNode(MNQuery.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return mnQuery.listQueryEngines(session);
            }
        }
        throw new ClientSideException("Call to listQueryEngines failed. " + node.getType()
                + " of version " + version);
    }
}
