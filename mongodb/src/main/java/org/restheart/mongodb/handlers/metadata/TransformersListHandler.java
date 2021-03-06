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
package org.restheart.mongodb.handlers.metadata;

import io.undertow.server.HttpServerExchange;
import java.util.Arrays;
import java.util.List;
import org.bson.BsonValue;
import org.restheart.handlers.PipelinedHandler;
import org.restheart.handlers.exchange.RequestContext;
import org.restheart.plugins.mongodb.Transformer;
import org.restheart.plugins.mongodb.Transformer.PHASE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * handler that applies the transformers passed to the costructor
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class TransformersListHandler extends PipelinedHandler {

    static final Logger LOGGER
            = LoggerFactory.getLogger(TransformersListHandler.class);

    private final List<Transformer> transformers;
    private final PHASE phase;

    /**
     * Creates a new instance of TransformerHandler
     *
     * @param phase
     * @param transformers
     */
    public TransformersListHandler(
            PHASE phase,
            Transformer... transformers) {
        this(null, phase, transformers);
    }
    
    /**
     * Creates a new instance of TransformerHandler
     *
     * @param next
     * @param phase
     * @param transformers
     */
    public TransformersListHandler(
            PipelinedHandler next,
            PHASE phase,
            Transformer... transformers) {
        super(next);

        this.phase = phase;
        this.transformers = Arrays.asList(transformers);
    }

    /**
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        var context = RequestContext.wrap(exchange);
        
        if (doesTransformerAppy()) {
            transform(exchange, context);
        }

        next(exchange);
    }

    private boolean doesTransformerAppy() {
        return transformers != null
                && !transformers.isEmpty();
    }

    private void transform(
            HttpServerExchange exchange,
            RequestContext context)
            throws InvalidMetadataException {
        BsonValue data;

        if (this.phase == PHASE.REQUEST) {
            data = context.getContent();
        } else {
            data = context.getResponseContent();
        }

        transformers.stream().forEachOrdered(
                t -> t.transform(exchange, context, data, null));
    }
}
