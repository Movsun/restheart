package org.restheart.mongodb.handlers;

import com.google.common.annotations.VisibleForTesting;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.exchange.OperationResult;
import org.restheart.handlers.exchange.RequestContext;
import org.restheart.mongodb.db.Database;
import org.restheart.mongodb.db.DatabaseImpl;
import org.restheart.mongodb.handlers.metadata.InvalidMetadataException;
import org.restheart.mongodb.metadata.CheckerMetadata;
import org.restheart.mongodb.metadata.Relationship;
import org.restheart.mongodb.metadata.TransformerMetadata;
import org.restheart.mongodb.utils.ResponseHelper;
import org.restheart.utils.HttpStatus;

/**
 *
 * @deprecated use PipelinedHandler instead
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
@Deprecated
public abstract class PipedHttpHandler implements HttpHandler {

    /**
     *
     */
    protected static final String PROPERTIES = "properties";

    /**
     *
     */
    protected static final String FILE_METADATA = "metadata";

    /**
     *
     */
    protected static final String _ID = "_id";

    /**
     *
     */
    protected static final String CONTENT_TYPE = "contentType";

    /**
     *
     */
    protected static final String FILENAME = "filename";

    private final Database dbsDAO;
    private final PipedHttpHandler next;

    /**
     * Creates a default instance of PipedHttpHandler with next = null and
     * dbsDAO = new DbsDAO()
     */
    public PipedHttpHandler() {
        this(null, new DatabaseImpl());
    }

    /**
     *
     * @param next the next handler in this chain
     */
    public PipedHttpHandler(PipedHttpHandler next) {
        this(next, new DatabaseImpl());
    }

    /**
     * Inject a custom DbsDAO, usually a mock for testing purposes
     *
     * @param next
     * @param dbsDAO
     */
    @VisibleForTesting
    public PipedHttpHandler(PipedHttpHandler next, Database dbsDAO) {
        this.next = next;
        this.dbsDAO = dbsDAO;
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    public abstract void handleRequest(
            HttpServerExchange exchange,
            RequestContext context)
            throws Exception;

    /**
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        handleRequest(exchange, null);
    }

    /**
     * @return the dbsDAO
     */
    protected Database getDatabase() {
        return dbsDAO;
    }

    /**
     * @return the next PipedHttpHandler
     */
    protected PipedHttpHandler getNext() {
        return next;
    }

    /**
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    protected void next(HttpServerExchange exchange, RequestContext context) throws Exception {
        if (getNext() != null) {
            getNext().handleRequest(exchange, context);
        }
    }

    /**
     *
     * @param content
     * @param exchange
     * @param context
     * @return
     * @throws Exception
     */
    protected boolean isInvalidMetadata(BsonDocument content, HttpServerExchange exchange, RequestContext context) throws Exception {
        // check RELS metadata
        if (content.containsKey(Relationship.RELATIONSHIPS_ELEMENT_NAME)) {
            try {
                Relationship.getFromJson(content);
            } catch (InvalidMetadataException ex) {
                ResponseHelper.endExchangeWithMessage(
                        exchange,
                        HttpStatus.SC_NOT_ACCEPTABLE,
                        "wrong relationships definition. "
                        + ex.getMessage(), ex);
                next(exchange, context);
                return true;
            }
        }
        // check RT metadata
        if (content.containsKey(TransformerMetadata.RTS_ELEMENT_NAME)) {
            try {
                TransformerMetadata.getFromJson(content);
            } catch (InvalidMetadataException ex) {
                ResponseHelper.endExchangeWithMessage(
                        exchange,
                        HttpStatus.SC_NOT_ACCEPTABLE,
                        "wrong representation transformer definition. "
                        + ex.getMessage(), ex);
                next(exchange, context);
                return true;
            }
        }
        // check SC metadata
        if (content.containsKey(CheckerMetadata.ROOT_KEY)) {
            try {
                CheckerMetadata.getFromJson(content);
            } catch (InvalidMetadataException ex) {
                ResponseHelper.endExchangeWithMessage(
                        exchange,
                        HttpStatus.SC_NOT_ACCEPTABLE,
                        "wrong checker definition. "
                        + ex.getMessage(), ex);
                next(exchange, context);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param _content
     * @param exchange
     * @param context
     * @return
     * @throws Exception
     */
    protected boolean isNotAcceptableContent(BsonValue _content, HttpServerExchange exchange, RequestContext context) throws Exception {
        // cannot proceed with no data
        if (_content == null) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "no data provided");
            next(exchange, context);
            return true;
        }
        // cannot proceed with an array
        if (!_content.isDocument()) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "data must be a json object");
            next(exchange, context);
            return true;
        }
        if (_content.asDocument().isEmpty()) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "no data provided");
            next(exchange, context);
            return true;
        }
        return false;
    }

    /**
     *
     * @param context
     * @param result
     * @param exchange
     * @return
     * @throws Exception
     */
    protected boolean isResponseInConflict(RequestContext context, OperationResult result, HttpServerExchange exchange) throws Exception {
        context.setDbOperationResult(result);
        // inject the etag
        if (result.getEtag() != null) {
            ResponseHelper.injectEtagHeader(exchange, result.getEtag());
        }
        if (result.getHttpCode() == HttpStatus.SC_CONFLICT) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_CONFLICT,
                    "The ETag must be provided using the '"
                    + Headers.IF_MATCH
                    + "' header");
            next(exchange, context);
            return true;
        }
        // handle the case of duplicate key error
        if (result.getHttpCode() == HttpStatus.SC_EXPECTATION_FAILED) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_EXPECTATION_FAILED,
                    ResponseHelper.getMessageFromErrorCode(11000));
            next(exchange, context);
            return true;
        }
        return false;
    }
}
