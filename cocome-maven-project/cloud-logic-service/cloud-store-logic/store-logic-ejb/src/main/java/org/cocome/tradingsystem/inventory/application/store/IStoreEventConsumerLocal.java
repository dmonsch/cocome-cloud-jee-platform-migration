/***************************************************************************
 * Copyright 2013 DFG SPP 1593 (http://dfg-spp1593.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package org.cocome.tradingsystem.inventory.application.store;

import org.cocome.tradingsystem.cashdeskline.events.AccountSaleEvent;
import org.cocome.tradingsystem.inventory.data.persistence.UpdateException;
import org.cocome.tradingsystem.util.exception.NotInDatabaseException;

import javax.ejb.Local;
import javax.enterprise.event.Observes;

/**
 * Defines events handled asynchronously by the store inventory application.
 *
 * @author Lubomir Bulej
 * @author Tobias Pöppke
 * @author Robert Heinrich
 */
@Local
public interface IStoreEventConsumerLocal {

    /**
     * Registers the sale described in the event in the inventory system.
     *
     * @param event event describing the sale made at a cash desk
     * @throws ProductOutOfStockException
     * @throws NotInDatabaseException
     * @throws UpdateException
     */
    void onEvent(@Observes final AccountSaleEvent event)
            throws ProductOutOfStockException, NotInDatabaseException, UpdateException;

}
