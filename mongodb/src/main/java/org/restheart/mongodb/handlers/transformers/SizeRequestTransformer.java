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
package org.restheart.mongodb.handlers.transformers;

import io.undertow.server.HttpServerExchange;
import org.bson.BsonDocument;
import org.restheart.handlers.PipelinedHandler;
import org.restheart.handlers.exchange.BsonRequest;
import org.restheart.handlers.exchange.BsonResponse;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 *
 * During request phase, this transformer just sets the pagesize to 0 to avoid
 * retrieving data for count request. During response phase set the content to
 * just contain the _size property
 *
 */
public class SizeRequestTransformer extends PipelinedHandler {
    private final boolean phase;

    /**
     *
     * @param phase true for request phase, false for response
     */
    public SizeRequestTransformer(boolean phase) {
        this.phase = phase;
    }

    /**
     *
     * @param exchange
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // for request phase
        if (phase) {
            BsonRequest.wrap(exchange).setPagesize(0);
        } else {
            var response = BsonResponse.wrap(exchange);

            // for response phase
            if (response.getContent() != null
                    && response.getContent().isDocument()
                    && response.getContent().asDocument().containsKey("_size")) {

                var doc = response.getContent().asDocument();

                response.setContent(new BsonDocument("_size", doc.get("_size")));
            }
        }

        next(exchange);
    }
}
