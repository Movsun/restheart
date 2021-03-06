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
package org.restheart.mongodb.handlers.indexes;

import io.undertow.server.HttpServerExchange;
import org.restheart.handlers.PipelinedHandler;
import org.restheart.handlers.exchange.BsonRequest;
import org.restheart.handlers.exchange.BsonResponse;
import org.restheart.mongodb.db.DatabaseImpl;
import org.restheart.mongodb.utils.ResponseHelper;
import org.restheart.utils.HttpStatus;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class DeleteIndexHandler extends PipelinedHandler {
    private final DatabaseImpl dbsDAO = new DatabaseImpl();
    
    /**
     * Creates a new instance of DeleteIndexHandler
     */
    public DeleteIndexHandler() {
        super();
    }

    /**
     * Creates a new instance of DeleteIndexHandler
     *
     * @param next
     */
    public DeleteIndexHandler(PipelinedHandler next) {
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
        
        String dbName = request.getDBName();
        String collectionName = request.getCollectionName();

        String indexId = request.getIndexId();

        if (indexId.startsWith("_") || indexId.equals("_id_")) {
            ResponseHelper.endExchangeWithMessage(
                    exchange, 
                    HttpStatus.SC_UNAUTHORIZED, 
                    indexId + " is a default index and cannot be deleted");
            next(exchange);
            return;
        }

        int httpCode = dbsDAO.deleteIndex(
                request.getClientSession(),
                dbName, 
                collectionName, 
                indexId);
        
        response.setStatusCode(httpCode);
        
        next(exchange);
    }
}
