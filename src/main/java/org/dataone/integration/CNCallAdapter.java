package org.dataone.integration;

import java.io.InputStream;
import java.net.URI;

import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.cn.v2.CNRead;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;

/**
 * Subclass of {@link CommonCallAdapter} that can be used to call CNode API methods that
 * have similar structure and behavior across versions. Extends CommonCallAdapter
 * to allow tests to call methods sequentially from both with the same state.
 * </p><b>Warning:</b> Calls that are invalid (don't exist for a specific version, or don't apply to the node given) will throw a {@link ClientSideException}.
 */
public class CNCallAdapter extends CommonCallAdapter {

    public CNCallAdapter(MultipartRestClient mrc, Node node, String version) {
        super(mrc, node, version);
    }

    public ObjectLocationList resolve(Session session, Identifier pid) throws ClientSideException,
            InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.resolve(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                org.dataone.service.cn.v2.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v2.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.resolve(session, pid);
            }
        }
        throw new ClientSideException("Resolve call failed. " + node.getType() + " of version "
                + version);
    }

    public ObjectList search(Session session, String queryType, String query)
            throws ClientSideException, InvalidToken, ServiceFailure, NotAuthorized,
            InvalidRequest, NotImplemented {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.search(session, queryType, query);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.search(session, queryType, query);
            }
        }
        throw new ClientSideException("Search call failed. " + node.getType() + " of version "
                + version);
    }

    public InputStream query(Session session, String queryEngine, String query)
            throws ClientSideException, InvalidToken, ServiceFailure, NotAuthorized,
            InvalidRequest, NotImplemented, NotFound {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.query(queryEngine, query);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.query(session, queryEngine, query);
            }
        }
        throw new ClientSideException("Query call failed. " + node.getType() + " of version "
                + version);
    }

    public DescribeResponse describe(Session session, Identifier pid) throws InvalidToken,
            NotAuthorized, NotImplemented, ServiceFailure, NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.describe(session, pid);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.describe(session, pid);
            }
        }
        throw new ClientSideException("Describe call failed. " + node.getType() + " of version "
                + version);
    }

    public QueryEngineList listQueryEngines(Session session) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.listQueryEngines();
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.listQueryEngines(session);
            }
        }
        throw new ClientSideException("Fetching list of query engines failed. " + node.getType()
                + " of version " + version);
    }

    public QueryEngineDescription getQueryEngineDescription(Session session, String queryEngine)
            throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, NotFound,
            ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRead cnRead = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.getQueryEngineDescription(queryEngine);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRead cnRead = D1NodeFactory.buildNode(CNRead.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRead.getQueryEngineDescription(session, queryEngine);
            }
        }
        throw new ClientSideException("Fetching query engine descriptions failed. "
                + node.getType() + " of version " + version);
    }

}
