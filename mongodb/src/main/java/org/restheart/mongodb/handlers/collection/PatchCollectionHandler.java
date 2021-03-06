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
package org.restheart.mongodb.handlers.collection;

import io.undertow.server.HttpServerExchange;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.restheart.handlers.PipelinedHandler;
import org.restheart.handlers.exchange.BsonRequest;
import org.restheart.handlers.exchange.BsonResponse;
import static org.restheart.handlers.exchange.ExchangeKeys._SCHEMAS;
import org.restheart.handlers.exchange.OperationResult;
import org.restheart.mongodb.db.DatabaseImpl;
import org.restheart.mongodb.handlers.injectors.LocalCachesSingleton;
import org.restheart.mongodb.utils.RequestHelper;
import org.restheart.mongodb.utils.ResponseHelper;
import org.restheart.utils.HttpStatus;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class PatchCollectionHandler extends PipelinedHandler {
    private final DatabaseImpl dbsDAO = new DatabaseImpl();

    /**
     * Creates a new instance of PatchCollectionHandler
     */
    public PatchCollectionHandler() {
        super();
    }

    /**
     * Creates a new instance of PatchCollectionHandler
     *
     * @param next
     */
    public PatchCollectionHandler(PipelinedHandler next) {
        super(next);
    }

    /**
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        var request = BsonRequest.wrap(exchange);
        var response = BsonResponse.wrap(exchange);
        
        if (request.isInError()) {
            next(exchange);
            return;
        }

        if (request.getDBName().isEmpty()) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "wrong request, db name cannot be empty");
            next(exchange);
            return;
        }

        if (request.getCollectionName().isEmpty()
                || (request.getCollectionName().startsWith("_")
                && !request.getCollectionName().equals(_SCHEMAS))) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "wrong request, collection name cannot be "
                    + "empty or start with _");
            next(exchange);
            return;
        }

        BsonValue _content = request.getContent();

        if (_content == null) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "no data provided");
            next(exchange);
            return;
        }

        // cannot PATCH with an array
        if (!_content.isDocument()) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "data must be a json object");
            next(exchange);
            return;
        }

        if (_content.asDocument().isEmpty()) {
            ResponseHelper.endExchangeWithMessage(
                    exchange,
                    HttpStatus.SC_NOT_ACCEPTABLE,
                    "no data provided");
            next(exchange);
            return;
        }

        final BsonDocument content = _content.asDocument();

        if (RequestHelper.isInvalidMetadata(content, exchange)) {
            next(exchange);
            return;
        }

        OperationResult result = dbsDAO
                .upsertCollection(
                        request.getClientSession(),
                        request.getDBName(),
                        request.getCollectionName(),
                        content,
                        request.getETag(),
                        true,
                        true,
                        request.isETagCheckRequired());

        if (RequestHelper.isResponseInConflict(result, exchange)) {
            next(exchange);
            return;
        }

        response.setStatusCode(result.getHttpCode());

        LocalCachesSingleton.getInstance()
                .invalidateCollection(
                        request.getDBName(),
                        request.getCollectionName());

        next(exchange);
    }

}
