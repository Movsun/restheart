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
package org.restheart.mongodb.handlers.aggregation;

import io.undertow.server.HttpServerExchange;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.restheart.handlers.exchange.BsonRequest;
import org.restheart.mongodb.handlers.IllegalQueryParamenterException;
import org.restheart.mongodb.representation.AbstractRepresentationFactory;
import org.restheart.mongodb.representation.Link;
import org.restheart.mongodb.representation.Resource;
import org.restheart.mongodb.utils.URLUtils;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class AggregationResultRepresentationFactory
        extends AbstractRepresentationFactory {

    /**
     *
     */
    public AggregationResultRepresentationFactory() {
    }

    /**
     *
     * @param exchange
     * @param embeddedData
     * @param size
     * @return
     * @throws IllegalQueryParamenterException
     */
    @Override
    public Resource getRepresentation(HttpServerExchange exchange,
            List<BsonDocument> embeddedData,
            long size)
            throws IllegalQueryParamenterException {
        var request = BsonRequest.wrap(exchange);

        final String requestPath = buildRequestPath(exchange);
        final Resource rep;

        if (request.isFullHalMode()) {
            rep = createRepresentation(exchange, requestPath);
        } else {
            rep = createRepresentation(exchange, null);
        }

        addSize(size, rep);

        addEmbeddedData(embeddedData, rep);

        if (request.isFullHalMode()) {
            addLinkTemplates(rep, requestPath);
        }

        return rep;
    }

    private void addEmbeddedData(List<BsonDocument> embeddedData,
            final Resource rep)
            throws IllegalQueryParamenterException {
        if (embeddedData != null) {
            addReturnedProperty(embeddedData, rep);

            if (!embeddedData.isEmpty()) {
                embeddedDocuments(embeddedData, rep);
            }
        } else {
            rep.addProperty("_returned", new BsonInt32(0));
        }
    }

    private void addLinkTemplates(final Resource rep,
            final String requestPath) {
        rep.addLink(new Link("rh:collection",
                URLUtils.getParentPath(URLUtils.getParentPath(requestPath))));
    }

    private void embeddedDocuments(List<BsonDocument> embeddedData,
            Resource rep) throws IllegalQueryParamenterException {
        embeddedData.stream().map((d) -> {
            Resource nrep = new Resource();
            nrep.addProperties(d);
            return nrep;
        }).forEach((nrep) -> {
            rep.addChild("rh:result", nrep);
        });
    }

    /**
     *
     * @param size
     * @param rep
     */
    protected void addSize(
            final long size,
            final Resource rep) {
        if (size == 0) {
            rep.addProperty("_size", new BsonInt32(0));
        }
    }
}
