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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Session;
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
        
        if (this.node.getType().equals(NodeType.MN) 
                && this.version.toLowerCase().equals("v2")) {
            
            MNRead mnRead = D1NodeFactory.buildNode(MNRead.class, this.mrc,
                    URI.create(this.node.getBaseURL()));
            return mnRead.systemMetadataChanged(session, id, serialVersion,
                    dateSystemMetadataLastModified);
        }
        throw new ClientSideException("Call to systemMetaDataChanged failed. " + node.getType()
                + " of version " + version);
    }

    public boolean synchronizationFailed(Session session, SynchronizationFailed message)
            throws InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, ClientSideException {

        if (this.node.getType().equals(NodeType.CN)) {
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

        if (this.node.getType().equals(NodeType.CN)) {
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
            InvalidToken, NotAuthorized, NotImplemented, ServiceFailure, UnsupportedType {
        
        return null;
    }

}
