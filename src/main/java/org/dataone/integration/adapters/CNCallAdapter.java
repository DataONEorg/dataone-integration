package org.dataone.integration.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;

import org.dataone.client.D1NodeFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.cn.v2.CNIdentity;
import org.dataone.service.cn.v2.CNAuthorization;
import org.dataone.service.cn.v2.CNCore;
import org.dataone.service.cn.v2.CNRead;
import org.dataone.service.cn.v2.CNRegister;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;

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

    public Identifier setRightsHolder(Session session, Identifier pid, Subject userId,
            long serialVersion) throws ClientSideException, InvalidToken, ServiceFailure, NotFound,
            NotAuthorized, NotImplemented, InvalidRequest, VersionMismatch {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNAuthorization cnAuth = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnAuth.setRightsHolder(session, pid, userId, serialVersion);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNAuthorization cnAuth = D1NodeFactory.buildNode(CNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnAuth.setRightsHolder(session, pid, userId, serialVersion);
            }
        }
        throw new ClientSideException("Call to setRightsHolder failed. " + node.getType()
                + " of version " + version);
    }

    public boolean setAccessPolicy(Session session, Identifier pid, AccessPolicy accessPolicy,
            long serialVersion) throws ClientSideException, InvalidToken, NotFound, NotImplemented,
            NotAuthorized, ServiceFailure, InvalidRequest, VersionMismatch {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNAuthorization cnAuth = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnAuth.setAccessPolicy(session, pid, accessPolicy, serialVersion);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNAuthorization cnAuth = D1NodeFactory.buildNode(CNAuthorization.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnAuth.setAccessPolicy(session, pid, accessPolicy, serialVersion);
            }
        }
        throw new ClientSideException("Call to setAccessPolicy failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier create(Session session, Identifier pid, InputStream object,
            SystemMetadata sysmeta) throws ClientSideException, InvalidToken, ServiceFailure,
            NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources,
            InvalidSystemMetadata, NotImplemented, InvalidRequest {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.create(session, pid, object, sysmeta);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.create(session, pid, object, sysmeta);
            }
        }
        throw new ClientSideException("Call to create failed. " + node.getType() + " of version "
                + version);
    }

    public Subject registerAccount(Session session, Person person) throws ServiceFailure,
            NotAuthorized, IdentifierNotUnique, InvalidCredentials, NotImplemented, InvalidRequest,
            InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.registerAccount(session, person);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.registerAccount(session, person);
            }
        }
        throw new ClientSideException("Call to registerAccount failed. " + node.getType()
                + " of version " + version);
    }

    public Subject updateAccount(Session session, Person person) throws ServiceFailure,
            NotAuthorized, InvalidCredentials, NotImplemented, InvalidRequest, InvalidToken,
            NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.updateAccount(session, person);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.updateAccount(session, person);
            }
        }
        throw new ClientSideException("Call to updateAccount failed. " + node.getType()
                + " of version " + version);
    }

    public boolean verifyAccount(Session session, Subject subject) throws ServiceFailure,
            NotAuthorized, NotImplemented, InvalidToken, InvalidRequest, NotFound,
            ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.verifyAccount(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.verifyAccount(session, subject);
            }
        }
        throw new ClientSideException("Call to verifyAccount failed. " + node.getType()
                + " of version " + version);
    }

    public SubjectInfo getSubjectInfo(Session session, Subject subject) throws ServiceFailure,
            NotAuthorized, NotImplemented, NotFound, InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.getSubjectInfo(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.getSubjectInfo(session, subject);
            }
        }
        throw new ClientSideException("Call to getSubjectInfo failed. " + node.getType()
                + " of version " + version);
    }

    public SubjectInfo listSubjects(Session session, String query, String status, Integer start,
            Integer count) throws InvalidRequest, ServiceFailure, InvalidToken, NotAuthorized,
            NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.listSubjects(session, query, status, start, count);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.listSubjects(session, query, status, start, count);
            }
        }
        throw new ClientSideException("Call to listSubjects failed. " + node.getType()
                + " of version " + version);
    }

    public boolean mapIdentity(Session session, Subject primarySubject, Subject secondarySubject)
            throws ServiceFailure, InvalidToken, NotAuthorized, NotFound, NotImplemented,
            InvalidRequest, IdentifierNotUnique, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.mapIdentity(session, primarySubject, secondarySubject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.mapIdentity(session, primarySubject, secondarySubject);
            }
        }
        throw new ClientSideException("Call to mapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public boolean requestMapIdentity(Session session, Subject subject) throws ServiceFailure,
            InvalidToken, NotAuthorized, NotFound, NotImplemented, InvalidRequest,
            IdentifierNotUnique, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.requestMapIdentity(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.requestMapIdentity(session, subject);
            }
        }
        throw new ClientSideException("Call to requestMapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public boolean confirmMapIdentity(Session session, Subject subject) throws ServiceFailure,
            InvalidToken, NotAuthorized, NotFound, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.confirmMapIdentity(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.confirmMapIdentity(session, subject);
            }
        }
        throw new ClientSideException("Call to confirmMapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public SubjectInfo getPendingMapIdentity(Session session, Subject subject)
            throws ServiceFailure, InvalidToken, NotAuthorized, NotFound, NotImplemented,
            ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.getPendingMapIdentity(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.getPendingMapIdentity(session, subject);
            }
        }
        throw new ClientSideException("Call to getPendingMapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public boolean denyMapIdentity(Session session, Subject subject) throws ServiceFailure,
            InvalidToken, NotAuthorized, NotFound, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.denyMapIdentity(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.denyMapIdentity(session, subject);
            }
        }
        throw new ClientSideException("Call to denyMapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public boolean removeMapIdentity(Session session, Subject subject) throws ServiceFailure,
            InvalidToken, NotAuthorized, NotFound, NotImplemented, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.removeMapIdentity(session, subject);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.removeMapIdentity(session, subject);
            }
        }
        throw new ClientSideException("Call to removeMapIdentity failed. " + node.getType()
                + " of version " + version);
    }

    public Subject createGroup(Session session, Group group) throws ServiceFailure, InvalidToken,
            NotAuthorized, NotImplemented, IdentifierNotUnique, InvalidRequest, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.createGroup(session, group);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.createGroup(session, group);
            }
        }
        throw new ClientSideException("Call to createGroup failed. " + node.getType()
                + " of version " + version);
    }

    public boolean updateGroup(Session session, Group group) throws ServiceFailure, InvalidToken,
            NotAuthorized, NotFound, NotImplemented, InvalidRequest, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNIdentity cnIdentify = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.updateGroup(session, group);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNIdentity cnIdentify = D1NodeFactory.buildNode(CNIdentity.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnIdentify.updateGroup(session, group);
            }
        }
        throw new ClientSideException("Call to updateGroup failed. " + node.getType()
                + " of version " + version);
    }

    public ObjectFormatList listFormats() throws ServiceFailure, NotImplemented,
            ClientSideException, InstantiationException, IllegalAccessException,
            InvocationTargetException, JiBXException, IOException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                org.dataone.service.types.v1.ObjectFormatList formatListV1 = cnCore.listFormats();
                ObjectFormatList formatListV2 = TypeMarshaller.convertTypeFromType(formatListV1,
                        ObjectFormatList.class);
                return formatListV2;
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.listFormats();
            }
        }
        throw new ClientSideException("Call to listFormats failed. " + node.getType()
                + " of version " + version);
    }

    public ObjectFormat getFormat(ObjectFormatIdentifier formatid) throws ServiceFailure, NotFound,
            NotImplemented, InvalidRequest, ClientSideException, InstantiationException,
            IllegalAccessException, InvocationTargetException, JiBXException, IOException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                org.dataone.service.types.v1.ObjectFormat formatV1 = cnCore.getFormat(formatid);
                ObjectFormat formatV2 = TypeMarshaller.convertTypeFromType(formatV1,
                        ObjectFormat.class);
                return formatV2;
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.getFormat(formatid);
            }
        }
        throw new ClientSideException("Call to getFormat failed. " + node.getType()
                + " of version " + version);
    }

    public ChecksumAlgorithmList listChecksumAlgorithms() throws ServiceFailure, NotImplemented,
            ClientSideException, InstantiationException, IllegalAccessException,
            InvocationTargetException, JiBXException, IOException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.listChecksumAlgorithms();
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.listChecksumAlgorithms();
            }
        }
        throw new ClientSideException("Call to listChecksumAlgorithms failed. " + node.getType()
                + " of version " + version);
    }

    public NodeList listNodes() throws NotImplemented, ServiceFailure, ClientSideException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            JiBXException, IOException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                org.dataone.service.types.v1.NodeList nodeListV1 = cnCore.listNodes();
                NodeList nodeListV2 = TypeMarshaller
                        .convertTypeFromType(nodeListV1, NodeList.class);
                return nodeListV2;
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.listNodes();
            }
        }
        throw new ClientSideException("Call to listNodes failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier reserveIdentifier(Session session, Identifier id) throws InvalidToken,
            ServiceFailure, NotAuthorized, IdentifierNotUnique, NotImplemented, InvalidRequest,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            JiBXException, IOException, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.reserveIdentifier(session, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.reserveIdentifier(session, id);
            }
        }
        throw new ClientSideException("Call to reserveIdentifier failed. " + node.getType()
                + " of version " + version);
    }

    public org.dataone.service.types.v2.Node getCapabilities() throws NotImplemented,
            ServiceFailure, ClientSideException {
        if (this.node.getType().equals(NodeType.CN) 
                && this.version.toLowerCase().equals("v2")) {
            CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                    URI.create(this.node.getBaseURL()));
            return cnCore.getCapabilities();
        }
        throw new ClientSideException("Call to getCapabilities failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier generateIdentifier(Session session, String scheme, String fragment)
            throws InvalidToken, ServiceFailure, NotAuthorized, NotImplemented, InvalidRequest,
            ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.generateIdentifier(session, scheme, fragment);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.generateIdentifier(session, scheme, fragment);
            }
        }
        throw new ClientSideException("Call to generateIdentifier failed. " + node.getType()
                + " of version " + version);
    }

    public boolean hasReservation(Session session, Subject subject, Identifier id)
            throws InvalidToken, ServiceFailure, NotFound, NotAuthorized, NotImplemented,
            InvalidRequest, IdentifierNotUnique, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.hasReservation(session, subject, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.hasReservation(session, subject, id);
            }
        }
        throw new ClientSideException("Call to hasReservation failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier registerSystemMetadata(Session session, Identifier pid, SystemMetadata sysmeta)
            throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
            InvalidSystemMetadata, InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.registerSystemMetadata(session, pid, sysmeta);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.registerSystemMetadata(session, pid, sysmeta);
            }
        }
        throw new ClientSideException("Call to registerSystemMetadata failed. " + node.getType()
                + " of version " + version);
    }

    public boolean updateSystemMetadata(Session session, Identifier pid, SystemMetadata sysmeta)
            throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest,
            InvalidSystemMetadata, InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.CN) 
                && this.version.toLowerCase().equals("v2")) {
            CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                    URI.create(this.node.getBaseURL()));
            return cnCore.updateSystemMetadata(session, pid, sysmeta);
        }
        throw new ClientSideException("Call to updateSystemMetadata failed. " + node.getType()
                + " of version " + version);
    }

    public boolean setObsoletedBy(Session session, Identifier pid, Identifier obsoletedByPid,
            long serialVersion) throws NotImplemented, NotFound, NotAuthorized, ServiceFailure,
            InvalidRequest, InvalidToken, VersionMismatch, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.setObsoletedBy(session, pid, obsoletedByPid, serialVersion);
            }
        }
        throw new ClientSideException("Call to setObsoletedBy failed. " + node.getType()
                + " of version " + version);
    }

    public Identifier delete(Session session, Identifier id) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, ClientSideException, InvalidRequest {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.delete(session, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.delete(session, id);
            }
        }
        throw new ClientSideException("Call to delete failed. " + node.getType() + " of version "
                + version);
    }

    public Identifier archive(Session session, Identifier id) throws InvalidToken, ServiceFailure,
            NotAuthorized, NotFound, NotImplemented, ClientSideException, InvalidRequest {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNCore cnCore = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.archive(session, id);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNCore cnCore = D1NodeFactory.buildNode(CNCore.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnCore.archive(session, id);
            }
        }
        throw new ClientSideException("Call to archive failed. " + node.getType() + " of version "
                + version);
    }

    public boolean updateNodeCapabilities(Session session, NodeReference nodeid,
            org.dataone.service.types.v2.Node node) throws NotImplemented, NotAuthorized,
            ServiceFailure, InvalidRequest, NotFound, InvalidToken, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRegister cnRegister = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRegister.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRegister.updateNodeCapabilities(session, nodeid, node);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRegister cnRegister = D1NodeFactory.buildNode(CNRegister.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRegister.updateNodeCapabilities(session, nodeid, node);
            }
        }
        throw new ClientSideException("Call to updateNodeCapabilities failed. " + node.getType()
                + " of version " + version);
    }

    public NodeReference register(Session session, org.dataone.service.types.v2.Node node)
            throws NotImplemented, NotAuthorized, ServiceFailure, InvalidRequest, InvalidToken,
            IdentifierNotUnique, ClientSideException {
        if (this.node.getType().equals(NodeType.CN)) {
            if (this.version.toLowerCase().equals("v1")) {
                org.dataone.service.cn.v1.CNRegister cnRegister = D1NodeFactory.buildNode(
                        org.dataone.service.cn.v1.CNRegister.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRegister.register(session, node);
            } else if (this.version.toLowerCase().equals("v2")) {
                CNRegister cnRegister = D1NodeFactory.buildNode(CNRegister.class, this.mrc,
                        URI.create(this.node.getBaseURL()));
                return cnRegister.register(session, node);
            }
        }
        throw new ClientSideException("Call to register failed. " + node.getType() + " of version "
                + version);
    }

    public org.dataone.service.types.v2.Node getNodeCapabilities(NodeReference nodeid)
            throws NotImplemented, ServiceFailure, InvalidRequest, NotFound, ClientSideException {
        if (this.node.getType().equals(NodeType.CN) && this.version.toLowerCase().equals("v2")) {
            CNRegister cnRegister = D1NodeFactory.buildNode(CNRegister.class, this.mrc,
                    URI.create(this.node.getBaseURL()));
            return cnRegister.getNodeCapabilities(nodeid);
        }
        throw new ClientSideException("Call to getNodeCapabilities failed. " + node.getType()
                + " of version " + version);
    }

    // attempting to get rid of the copy pasta that is the adapter class code
    // shortens adapter calls a ton, 
    // but only works for methods that with same parameters
    // and it's not too neat :[

    //    private static final Class<?> CN_V1_IDENTITY = org.dataone.service.cn.v1.CNIdentity.class;
    //    private static final Class<?> CN_V2_IDENTITY = org.dataone.service.cn.v2.CNIdentity.class;
    //    
    //    private static HashMap<String, Class<?>> nodeInterfaceMap = new HashMap<String, Class<?>>();
    //    
    //    static{
    //        nodeInterfaceMap.put(getInterfaceKey(NodeType.CN, "v1", CN_V1_IDENTITY.getSimpleName()), CN_V1_IDENTITY);
    //        nodeInterfaceMap.put(getInterfaceKey(NodeType.CN, "v2", CN_V2_IDENTITY.getSimpleName()), CN_V2_IDENTITY);
    //        // ...
    //    }
    //
    //    private static String getInterfaceKey(NodeType nodeType, String version, String interfaceClassName) {
    //        return "" + nodeType + "v1" + interfaceClassName;
    //    }
    //    
    //    private <T> T runMethod(String interfaceClassName, String methodName, Class<T> returnClass, Object... params) throws ClientSideException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    //
    //        String interfaceKey = getInterfaceKey(node.getType(), methodName, interfaceClassName);
    //        Class<?> interfaceClass = nodeInterfaceMap.get(interfaceKey);
    //        
    //        Object interfaceInstance = D1NodeFactory.buildNode(
    //                interfaceClass, this.mrc,
    //                URI.create(this.node.getBaseURL()));
    //        
    //        Class<?>[] paramClasses = new Class<?>[params.length];
    //        for (int i=0; i<params.length; i++)
    //            paramClasses[i] = params[i].getClass();
    //        
    //        Method method = interfaceClass.getClass().getMethod(methodName, paramClasses);
    //        Object result = method.invoke(interfaceInstance, params);
    //        
    //        if(returnClass.isAssignableFrom(result.getClass()))
    //            return returnClass.cast(result);
    //        return null;
    //    }
    //    
    //    
    //    public boolean verifyAccount(Subject subject) throws ServiceFailure, NotAuthorized,
    //            NotImplemented, InvalidToken, InvalidRequest, NotFound, ClientSideException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    //        return runMethod("CNIdentity", "verifyAccount", boolean.class, subject);
    //    }
    //    
    // idea 2
    //    private static class AdapterInvocationHandler implements InvocationHandler {
    //
    //        private Object target;
    //        private Class<?> targetClass;
    //
    //        public AdapterInvocationHandler(Object target) {
    //            this.target = target;
    //            targetClass = target.getClass();
    //        }
    //
    //        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    //            try {
    //                Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
    //                if (!method.getReturnType().isAssignableFrom(targetMethod.getReturnType()))
    //                    throw new UnsupportedOperationException("Target (" + target.getClass().getName() + ") does not support: " + method.toGenericString());
    //                return targetMethod.invoke(target, args);
    //            } catch (NoSuchMethodException ex) {
    //                throw new UnsupportedOperationException("Target (" + target.getClass().getName() + ") does not support: " + method.toGenericString());
    //            } catch (IllegalAccessException ex) {
    //                throw new UnsupportedOperationException("Target (" + target.getClass().getName() + ") does not declare method to be public: " + method.toGenericString());
    //            } catch (InvocationTargetException ex) {
    //                // May throw a NullPointerException if there is no target exception
    //                throw ex.getTargetException();
    //            }
    //        }
    //    }
    //    
    //    private static class AdapterFactory {
    //
    //        public static <T> T createAdapter(Object target, Class<T> interfaceClass) {
    //            if (!interfaceClass.isInterface())
    //                throw new IllegalArgumentException("Must be an interface: " + interfaceClass.getName());
    //            return (T) Proxy.newProxyInstance(null, new Class<?>[] { interfaceClass }, new AdapterInvocationHandler(target));
    //        }
    //    }

}