/*
 * RESTHeart - the Web API for MongoDB
 * Copyright (C) SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.mongodb.utils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.bson.types.ObjectId;
import org.restheart.handlers.exchange.BsonRequest;
import org.restheart.handlers.exchange.BsonResponse;
import org.restheart.mongodb.representation.Resource;
import org.restheart.utils.HttpStatus;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class ResponseHelper {

    /**
     *
     * @param exchange
     * @param code
     * @param message
     */
    public static void endExchangeWithMessage(
            HttpServerExchange exchange,
            int code,
            String message) {
        endExchangeWithMessage(exchange, code, message, null);
    }

    /**
     *
     * @param exchange
     * @param code
     * @param message
     * @param t
     */
    public static void endExchangeWithMessage(
            HttpServerExchange exchange,
            int code,
            String message,
            Throwable t) {
        var request = BsonRequest.wrap(exchange);
        var response = BsonResponse.wrap(exchange);

        response.setStatusCode(code);

        String httpStatusText = HttpStatus.getStatusText(code);

        request.setInError(true);

        response.setContent(getErrorJsonDocument(
                exchange.getRequestPath(),
                code,
                response,
                httpStatusText,
                message,
                t, false)
                .asBsonDocument());
    }

    /**
     *
     * @param exchange
     * @param code
     * @param rep
     */
    public static void endExchangeWithRepresentation(
            HttpServerExchange exchange,
            int code,
            Resource rep) {
        var request = BsonRequest.wrap(exchange);
        var response = BsonResponse.wrap(exchange);

        request.setInError(true);
        response.setStatusCode(code);
        response.setContent(rep.asBsonDocument());
    }

    /**
     *
     * @param href
     * @param code
     * @param response
     * @param httpStatusText
     * @param message
     * @param t
     * @param includeStackTrace
     * @return
     */
    public static Resource getErrorJsonDocument(String href,
            int code,
            BsonResponse response,
            String httpStatusText,
            String message,
            Throwable t,
            boolean includeStackTrace) {
        Resource rep = new Resource(href);

        rep.addProperty("http status code",
                new BsonInt32(code));
        rep.addProperty("http status description",
                new BsonString(httpStatusText));
        if (message != null) {
            rep.addProperty(
                    "message",
                    new BsonString(avoidEscapedChars(message)));
        }

        Resource nrep = new Resource();

        if (t != null) {
            nrep.addProperty(
                    "exception",
                    new BsonString(t.getClass().getName()));

            if (t.getMessage() != null) {
                if (t instanceof JsonParseException) {
                    nrep.addProperty("exception message",
                            new BsonString("invalid json"));
                } else {
                    nrep.addProperty("exception message",
                            new BsonString(
                                    avoidEscapedChars(t.getMessage())));
                }

            }

            if (includeStackTrace) {
                BsonArray stackTrace = getStackTraceJson(t);

                if (stackTrace != null) {
                    nrep.addProperty("stack trace", stackTrace);
                }
            }

            rep.addChild("rh:exception", nrep);
        }

        // add warnings
        if (response != null
                && response.getWarnings() != null) {
            response.getWarnings().forEach(w -> rep.addWarning(w));
        }

        return rep;
    }

    private static BsonArray getStackTraceJson(Throwable t) {
        if (t == null || t.getStackTrace() == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String st = sw.toString();
        st = avoidEscapedChars(st);
        String[] lines = st.split("\n");

        BsonArray list = new BsonArray();

        for (String line : lines) {
            list.add(new BsonString(line));
        }

        return list;
    }

    private static String avoidEscapedChars(String s) {
        return s == null
                ? null
                : s
                        .replaceAll("\"", "'")
                        .replaceAll("\t", "  ");
    }

    /**
     * Set the ETag in the response's header
     *
     * @param exchange
     * @param etag
     */
    protected static void setETagHeader(final HttpServerExchange exchange, final String etag) {
        exchange.getResponseHeaders().put(Headers.ETAG, etag);
    }

    /**
     *
     * @param exchange
     * @param properties
     */
    public static void injectEtagHeader(
            HttpServerExchange exchange,
            BsonDocument properties) {
        if (properties == null) {
            return;
        }

        BsonValue _etag = properties.get("_etag");

        if (_etag == null) {
            return;
        }

        if (_etag.isObjectId()) {
            setETagHeader(exchange, _etag.asObjectId().getValue().toString());
        } else if (_etag.isString()) {
            setETagHeader(exchange, _etag.asString().getValue());
        }
    }

    /**
     *
     * @param exchange
     * @param properties
     */
    public static void injectEtagHeader(
            HttpServerExchange exchange,
            Document properties) {
        if (properties == null) {
            return;
        }

        Object _etag = properties.get("_etag");

        if (_etag == null) {
            return;
        }

        if (_etag instanceof ObjectId) {
            setETagHeader(exchange, _etag.toString());
        } else if (_etag instanceof String) {
            setETagHeader(exchange, (String) _etag);
        }
    }

    /**
     *
     * @param exchange
     * @param etag
     */
    public static void injectEtagHeader(
            HttpServerExchange exchange,
            Object etag) {

        if (etag == null) {
            return;
        }

        if (etag instanceof BsonValue) {
            BsonValue _etag = (BsonValue) etag;

            if (_etag.isObjectId()) {
                setETagHeader(exchange, _etag.asObjectId().getValue().toString());
            } else if (_etag.isString()) {
                setETagHeader(exchange, _etag.asString().getValue());
            }

        } else if (etag instanceof ObjectId) {
            setETagHeader(exchange, etag.toString());
        } else if (etag instanceof String) {
            setETagHeader(exchange, (String) etag);
        }

    }

    /**
     *
     * @param code mongodb error code from MongoException.getCode()
     * @return
     */
    public static int getHttpStatusFromErrorCode(int code) {
        switch (code) {
            case 13:
                // The MongoDB user does not have enough permissions to execute this operation.
                return HttpStatus.SC_FORBIDDEN;
            case 18:
                // Wrong MongoDB user credentials
                return HttpStatus.SC_FORBIDDEN;
            case 61:
                // Write request for sharded collection must specify the shardkey.
                return HttpStatus.SC_BAD_REQUEST;
            case 66:
                // Update tried to change the immutable shardkey
                return HttpStatus.SC_FORBIDDEN;
            case 121:
                // Document failed validation
                return HttpStatus.SC_BAD_REQUEST;
            case 112:
                //WriteConflict
                return HttpStatus.SC_CONFLICT;
            case 225:
            // Cannot start transaction X on session Y because a newer transaction Z has already started
            case 251:
            // transaction number X does not match any in-progress transactions
            case 256:
                // Transaction X has been committed.
                return HttpStatus.SC_NOT_ACCEPTABLE;
            case 11000:
                // error 11000 is duplicate key error
                // happens when the _id and a filter are specified,
                // the document exists but does not match the filter
                return HttpStatus.SC_CONFLICT;
            default:
                // Other
                return HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }
    }

    /**
     *
     * @param code mongodb error code from MongoException.getCode()
     * @return
     */
    public static String getMessageFromErrorCode(int code) {
        switch (code) {
            case 13:
                return "The MongoDB user does not have enough "
                        + "permissions to execute this operation";
            case 18:
                return "Wrong MongoDB user credentials "
                        + "(wrong password or need to specify the "
                        + "authentication dababase "
                        + "with 'authSource=<db>' option in mongo-uri)";
            case 61:
                return "Write request for sharded "
                        + "collection must specify the shardkey. "
                        + "Use the 'shardkey' query parameter";
            case 66:
                return "Update tried to change the immutable shardkey";
            case 121:
                //Document failed validation
                return "Document failed collection validation";
            case 112:
                //WriteConflict
                return "Write conflict inside transaction";
            case 225:
            // Cannot start transaction X on session Y because a newer transaction Z has already started// Cannot start transaction X on session Y because a newer transaction Z has already started
            case 251:
            // transaction number X does not match any in-progress transactions
            case 256:
                // Transaction X has been committed.
                return "The given transaction is not in-progress";
            case 11000:
                // error 11000 is duplicate key error
                // happens when the _id and a filter are specified,
                // the document exists but does not match the filter
                return "The document does not fulfill filter "
                        + "or index constraints";

            default:
                return "Error handling the request, "
                        + "see log for more information";
        }
    }

    private ResponseHelper() {
    }
}
