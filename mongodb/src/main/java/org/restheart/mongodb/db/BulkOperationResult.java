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
package org.restheart.mongodb.db;

import com.mongodb.bulk.BulkWriteResult;
import org.restheart.handlers.exchange.OperationResult;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class BulkOperationResult extends OperationResult {
    private final BulkWriteResult bulkResult;
    
    /**
     *
     * @param httpCode
     * @param etag
     * @param bulkResult
     */
    public BulkOperationResult(int httpCode, Object etag, 
            BulkWriteResult bulkResult) {
        super(httpCode, etag);
        
        this.bulkResult = bulkResult;
    }

    /**
     * @return the writeResult
     */
    public BulkWriteResult getBulkResult() {
        return bulkResult;
    }
}