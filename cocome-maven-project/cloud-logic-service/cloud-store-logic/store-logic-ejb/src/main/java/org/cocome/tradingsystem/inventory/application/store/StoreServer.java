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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.cocome.tradingsystem.inventory.data.enterprise.IEnterpriseDataFactory;
import org.cocome.tradingsystem.inventory.data.enterprise.IEnterpriseQuery;
import org.cocome.tradingsystem.inventory.data.enterprise.IProduct;
import org.cocome.tradingsystem.inventory.data.enterprise.IProductSupplier;
import org.cocome.tradingsystem.inventory.data.persistence.UpdateException;
import org.cocome.tradingsystem.inventory.data.persistence.IPersistenceContext;
import org.cocome.tradingsystem.inventory.data.store.IStoreQuery;
import org.cocome.tradingsystem.inventory.data.store.IOrderEntry;
import org.cocome.tradingsystem.inventory.data.store.IProductOrder;
import org.cocome.tradingsystem.inventory.data.store.IStockItem;
import org.cocome.tradingsystem.inventory.data.store.IStore;
import org.cocome.tradingsystem.inventory.data.store.IStoreDataFactory;
import org.cocome.tradingsystem.util.exception.NotInDatabaseException;
import org.cocome.tradingsystem.util.java.Lists;
import org.cocome.tradingsystem.util.java.Maps;
import org.cocome.tradingsystem.util.qualifier.StoreRequired;
import org.cocome.tradingsystem.util.scope.IContextRegistry;
import org.cocome.tradingsystem.util.scope.RegistryKeys;

import dmodel.pipeline.monitoring.controller.MonitoringMetadata;
import dmodel.pipeline.monitoring.controller.ServiceParameters;
import dmodel.pipeline.monitoring.controller.ThreadMonitoringController;
import dmodel.pipeline.monitoring.util.ManualMapping;

/**
 * Implements the server part of the store application.
 * 
 * @author Yannick Welsch
 * @author Lubomir Bulej
 * @author Tobias Pöppke
 * @author Robert Heinrich
 */
@Stateless
public class StoreServer implements Serializable, IStoreInventoryManagerLocal, IStoreInventoryLocal {
	private static final long serialVersionUID = -529765757261183369L;

	private static final Logger __log__ = Logger.getLogger(StoreServer.class);

	//

	@EJB
	private IStoreQuery __storeQuery;

	@EJB
	private IEnterpriseQuery __enterpriseQuery;

	@EJB
	private IPersistenceContext pctx;
	//

	/** Contains the identifier of the corresponding store entity. */
	@Inject
	@StoreRequired
	IContextRegistry context;

	@Inject
	IStoreDataFactory storeFactory;

	@Inject
	IEnterpriseDataFactory enterpriseFactory;

	/**
	 * Remote reference to the product dispatcher. TODO Change to webservice call
	 */
	// @EJB
	// private IProductDispatcherLocal __dispatcher;

	// private long __storeId;

	@PostConstruct
	private void __setUpStore() {
		long storeIndex = context.getLong(RegistryKeys.STORE_ID);
		__log__.debug("Setting up store with ID " + storeIndex);
	}

	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@ManualMapping("_RbkXkh_8Edy5k9ER1TBmjg")
	public ProductWithStockItemTO changePrice(long storeID, final StockItemTO stockItemTO)
			throws NotInDatabaseException, UpdateException {
		final IStockItem si = __storeQuery.queryStockItemById(stockItemTO.getId());

		si.setSalesPrice(stockItemTO.getSalesPrice());
		pctx.updateEntity(si);

		return storeFactory.fillProductWithStockItemTO(si);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_N8A88h_7Edy5k9ER1TBmjg")
	public List<ProductWithSupplierTO> getAllProducts(long storeID) throws NotInDatabaseException {
		final Collection<IProduct> products = __storeQuery.queryProducts(storeID);

		final List<ProductWithSupplierTO> result = Lists.newArrayList();
		for (final IProduct product : products) {
			result.add(enterpriseFactory.fillProductWithSupplierTO(product));
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_ZHuAYh_7Edy5k9ER1TBmjg")
	public List<ProductWithSupplierAndStockItemTO> getProductsWithStockItems(long storeID)
			throws NotInDatabaseException {
		final Collection<IStockItem> stockItems = __storeQuery.queryAllStockItems(storeID);

		final List<ProductWithSupplierAndStockItemTO> result = Lists.newArrayList();
		for (final IStockItem stockItem : stockItems) {
			result.add(storeFactory.fillProductWithSupplierAndStockItemTO(stockItem));
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_rLcrkh_7Edy5k9ER1TBmjg")
	public ComplexOrderTO getOrder(long storeID, final long orderId) throws NotInDatabaseException {
		return storeFactory.fillComplexOrderTO(__storeQuery.queryOrderById(orderId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ComplexOrderTO> getOutstandingOrders(long storeID) throws NotInDatabaseException {
		final Collection<IProductOrder> orders = __storeQuery.queryOutstandingOrders(storeID);

		final List<ComplexOrderTO> result = Lists.newArrayList();
		for (final IProductOrder order : orders) {
			result.add(storeFactory.fillComplexOrderTO(order));
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_H9phEh_7Edy5k9ER1TBmjg")
	public List<ProductWithStockItemTO> getProductsWithLowStock(long storeID) {
		final Collection<IStockItem> stockItems = __storeQuery.queryLowStockItems(storeID);

		final List<ProductWithStockItemTO> result = Lists.newArrayList();
		for (final IStockItem si : stockItems) {
			result.add(storeFactory.fillProductWithStockItemTO(si));
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("__vMR4h_6Edy5k9ER1TBmjg")
	public StoreWithEnterpriseTO getStore(long storeID) throws NotInDatabaseException {
		return storeFactory.fillStoreWithEnterpriseTO(__storeQuery.queryStoreById(storeID));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_iY7tEh_7Edy5k9ER1TBmjg")
	public List<ComplexOrderTO> orderProducts(long storeID, final ComplexOrderTO complexOrder)
			throws NotInDatabaseException, CreateException, UpdateException {
		final IStoreQuery sq = __storeQuery;

		final HashMap<Long, List<IOrderEntry>> ordersBySupplier = Maps.newHashMap();

		final LinkedList<IStockItem> updatedStockItems = new LinkedList<>();

		for (final ComplexOrderEntryTO coeto : complexOrder.getOrderEntryTOs()) {
			final IProduct product = sq.queryProductByBarcode(coeto.getProductTO().getBarcode());

			__debug("Found product %d", coeto.getProductTO().getBarcode());

			IOrderEntry oe = storeFactory.getNewOrderEntry();
			oe.setProduct(product);
			oe.setAmount(coeto.getAmount());
			oe.setProductBarcode(product.getBarcode());

			// this is probably not necessary to persist because it gets
			// persisted with the ProductOrder later on
			// pctx.createEntity(oe);

			//

			IProductSupplier supplier = product.getSupplier();
			long supplierId = -1;

			// It is possible that there is no supplier entry for a product...
			// Perhaps throw an exception in this case
			if (supplier != null) {
				supplierId = product.getSupplier().getId();
			}

			List<IOrderEntry> entries = ordersBySupplier.get(supplierId);
			if (entries == null) {
				entries = Lists.newArrayList();
				ordersBySupplier.put(supplierId, entries);
			}
			entries.add(oe);

			IStockItem item = sq.queryStockItem(storeID, oe.getProductBarcode());
			item.setIncomingAmount(item.getIncomingAmount() + oe.getAmount());
			updatedStockItems.add(item);
		}

		//

		System.out.println(ordersBySupplier);
		final IStore store = sq.queryStoreById(storeID);
		final List<IProductOrder> orders = Lists.newArrayList();
		for (final List<IOrderEntry> orderEntries : ordersBySupplier.values()) {
			IProductOrder po = storeFactory.getNewProductOrder();
			po.setOrderEntries(orderEntries);
			po.setStore(store);
			// set OrderingDate to NOW
			po.setOrderingDate(new Date());
			po.setDeliveryDate(null);

			pctx.createEntity(po);

			orders.add(po);
		}

		for (IStockItem item : updatedStockItems) {
			pctx.updateEntity(item);
		}

		//

		final List<ComplexOrderTO> result = Lists.newArrayList();
		for (final IProductOrder order : orders) {
			result.add(storeFactory.fillComplexOrderTO(order));
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@ManualMapping("_0MIB4h_7Edy5k9ER1TBmjg")
	public void rollInReceivedOrder(long storeID, final long orderId)
			throws InvalidRollInRequestException, NotInDatabaseException, UpdateException {
		final IProductOrder order = __storeQuery.queryOrderById(orderId);

		//
		// Ignore the roll in if the order has been already rolled in.
		//
		if (order.getDeliveryDate() != null && order.getDeliveryDate().after(order.getOrderingDate())) {
			final String message = String.format("Product order %d already rolled in.", order.getId());

			__warn(message);
			throw new InvalidRollInRequestException(message);
		}

		//
		// Ignore the roll in if the order is for different store.
		//
		if (order.getStore().getId() != storeID) {
			final String message = String.format("Order in store %d cannot be rolled-in by store %d",
					order.getStore().getId(), storeID);

			__error(message);
			throw new InvalidRollInRequestException(message);
		}

		// set DeliveryDate to NOW
		order.setDeliveryDate(new Date());

		for (final IOrderEntry oe : order.getOrderEntries()) {
			final IStockItem si = __storeQuery.queryStockItem(storeID, oe.getProduct().getBarcode());

			//
			// Create a new stock item for completely new products.
			//
			if (si == null) {
				// TODO Create a new stock item if it does not exist
			}

			final IProduct product = si.getProduct();
			final long oldAmount = si.getAmount();
			final long newAmount = oldAmount + oe.getAmount();
			final long newIncoming = si.getIncomingAmount() - oe.getAmount();

			si.setAmount(newAmount);
			if (newIncoming >= 0) {
				si.setIncomingAmount(newIncoming);
			} else {
				si.setIncomingAmount(0);
				__warn("New incoming amount of %s (%d) was negative (%d).", product.getName(), product.getBarcode(),
						newIncoming);
			}

			pctx.updateEntity(si);

			__debug("%s (%d) stock increased from %d to %d.", product.getName(), product.getBarcode(), oldAmount,
					newAmount);
		}
		pctx.updateEntity(order);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("_mAraYh_8Edy5k9ER1TBmjg")
	public ProductWithStockItemTO getProductWithStockItem(long storeID, final long productBarCode)
			throws NoSuchProductException {
		final IStockItem stockItem = __storeQuery.queryStockItem(storeID, productBarCode);

		if (stockItem == null) {
			throw new NoSuchProductException("There is no stock item for product with barcode " + productBarCode);
		}

		return storeFactory.fillProductWithStockItemTO(stockItem);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@ManualMapping("bookSale")
	public synchronized void accountSale(long storeID, final SaleTO sale)
			throws ProductOutOfStockException, NotInDatabaseException, UpdateException {
		// monitoring start
		ServiceParameters serviceParameters = new ServiceParameters();
		serviceParameters.addValue("saleTO.NUMBER_OF_ELEMENTS", sale.getProductTOs().size());
		serviceParameters.addValue("storeId.VALUE", String.valueOf(storeID));

		// session id
		String sessionId = UUID.randomUUID().toString();

		long startTime = ThreadMonitoringController.getInstance().getTime();
		try {
			synchronized (ThreadMonitoringController.getInstance()) {
				ThreadMonitoringController.setSessionId(sessionId);
				ThreadMonitoringController.getInstance().enterService("bookSale", this, serviceParameters);
			}

			// real call
			startTime = __bookSale(storeID, sale, ThreadMonitoringController.getInstance().getTime());

		} finally {
			ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.PERFORM_TRANSACTION,
					MonitoringMetadata.RESOURCE_CPU, startTime);

			synchronized (ThreadMonitoringController.getInstance()) {
				// monitoring end
				ThreadMonitoringController.getInstance().exitService("bookSale");
				// write overhead to file?
				ThreadMonitoringController.setSessionId("<not set>");
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	private long __bookSale(long storeID, final SaleTO saleTO, long startTime)
			throws ProductOutOfStockException, NotInDatabaseException, UpdateException {
		ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.PERSISTENCE_SETUP,
				MonitoringMetadata.RESOURCE_CPU, startTime);

		long iterations = 0;
		for (final ProductWithStockItemTO pwsto : saleTO.getProductTOs()) {
			final IStockItem si = __storeQuery.queryStockItemById(pwsto.getStockItemTO().getId());

			long startUpdate = ThreadMonitoringController.getInstance().getTime();
			long amount = si.getAmount();

			if (amount == 0) {
				ThreadMonitoringController.getInstance().logLoopIterationCount(MonitoringMetadata.PRODUCT_LOOP,
						iterations);
				// Normally this should not happen...
				throw new ProductOutOfStockException("The requested product is not in stock anymore!");
			}

			si.setAmount(si.getAmount() - 1);
			pctx.updateEntity(si);
			ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.UPDATE_ENTITY,
					MonitoringMetadata.RESOURCE_CPU, startUpdate);

			iterations++;
		}
		ThreadMonitoringController.getInstance().logLoopIterationCount(MonitoringMetadata.PRODUCT_LOOP, iterations);
		//
		// Check for items running low on stock. Required for UC 8.
		// Alternative (and probably better) design would be to check
		// once in a while from separate thread, not on every sale.
		//
		long tryStart = ThreadMonitoringController.getInstance().getTime();
		long innerDuration = tryStart;

		try {
			__checkForLowRunningGoods(storeID);
			innerDuration = ThreadMonitoringController.getInstance().getTime() - tryStart;
		} catch (final Exception e) {
			innerDuration = ThreadMonitoringController.getInstance().getTime() - tryStart;
			__warn("Failed UC8! Could not transport low-stock items from other stores: %s", e.getMessage());
		} finally {
			ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.TRY_BLOCK,
					MonitoringMetadata.RESOURCE_CPU, tryStart + innerDuration);
		}

		long result = ThreadMonitoringController.getInstance().getTime();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ComplexOrderEntryTO[] getStockItems(long storeID, final ProductTO[] requiredProductTOs)
			throws NotImplementedException {
		// TODO Auto-generated method stub
		throw new NotImplementedException("TODO: SDQ implement");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@ManualMapping("_YzMFYh_8Edy5k9ER1TBmjg")
	public void markProductsUnavailableInStock(long storeID, final ProductMovementTO movedProducts)
			throws ProductOutOfStockException, UpdateException {
		for (final ProductAmountTO movedProduct : movedProducts.getProductAmounts()) {
			final ProductTO productTO = movedProduct.getProduct();
			final long barcode = productTO.getBarcode();
			final IStockItem stockItem = __storeQuery.queryStockItem(storeID, barcode);

			if (stockItem == null) {
				throw new ProductOutOfStockException(
						String.format("Store %d has no product with barcode %d", storeID, barcode));
			}

			//

			final long availableAmount = stockItem.getAmount();
			final long movedAmount = movedProduct.getAmount();

			if (availableAmount < movedAmount) {
				throw new ProductOutOfStockException(
						String.format("Store %d only has %d product(s) with barcode %d, but %d required", storeID,
								availableAmount, barcode, movedAmount));
			}

			// set new remaining stock amount
			stockItem.setAmount(availableAmount - movedAmount);
			pctx.updateEntity(stockItem);

			//
			// TODO: virtual printout is missing
			// A list of all products that need to be delivered should be
			// printed out.
			//
			final StoreTO originStore = movedProducts.getOriginStore();
			final StoreTO destinationStore = movedProducts.getDestinationStore();
			System.out.printf("[%s at %s] Ship %s, barcode %d to %s at %s, amount %d\n", originStore.getName(),
					originStore.getLocation(), productTO.getName(), barcode, destinationStore.getName(),
					destinationStore.getLocation(), movedAmount);
		}
	}

	/**
	 * Checks for goods that run low. If there are goods running low they
	 * transported from nearby stores in the enterprise.
	 * <p>
	 * Technically, the operation is performed by the product dispatcher. The store
	 * only needs to provide it with products that are low on stock. If there is any
	 * problem communicating with the product dispatcher, the operation will not be
	 * performed. In case of transient errors, the operation may succeed during next
	 * check for low-stock products.
	 * <p>
	 * Required for UC 8
	 */
	private void __checkForLowRunningGoods(long storeID) throws Exception {
		//
		// Determine the products and amounts of items that are
		// actually required, i.e. items that are really low on
		// stock, including their current incoming amount.
		//
		final Collection<ProductAmountTO> requiredProducts = __findRequiredProducts(storeID);

		if (requiredProducts.size() < 1) {
			return;
		}

		//
		// Order required products from stores determined by the
		// product dispatcher.
		//
		final ProductAmountTO[] incomingProducts = __orderRequiredProducts(storeID, requiredProducts);

		if (incomingProducts.length < 1) {
			return;
		}

		//
		// Mark the products coming from other stores as incoming.
		//
		__registerIncomingProducts(storeID, incomingProducts);
	}

	private Collection<ProductAmountTO> __findRequiredProducts(long storeID) {
		//
		// Query the store inventory for apparently low stock items,
		// without consider items coming from other stores.
		//
		final Collection<IStockItem> lowStockItems = __storeQuery.queryLowStockItems(storeID);
		if (lowStockItems.size() < 1) {
			return Collections.emptyList();
		}

		//
		// Filter the low-stock items to determine items that are really
		// low on stock and should be transported from other stores.
		//
		// TODO this is not included in the SEFF, maybe adjust the SEFF
		final Collection<IStockItem> itemsToOrder = __selectItemsToOrder(storeID, lowStockItems);
		if (itemsToOrder.size() < 1) {
			return Collections.emptyList();
		}

		//
		// Finally determine the product amounts that need ot be transported
		// from nearby stores.
		//
		Collection<ProductAmountTO> ret = __calculateRequiredAmounts(storeID, itemsToOrder);
		return ret;
	}

	/**
	 * Selects and returns stock items that are really low on stock and will be
	 * ordered from other stores. Many items can be low on stock, but have more
	 * stock incoming that along with the current stock satisfies the minimal stock
	 * condition. Such items are filtered out and only those really low on stock are
	 * left.
	 */
	private Collection<IStockItem> __selectItemsToOrder(long storeID, final Collection<IStockItem> stockItems) {
		final Collection<IStockItem> result = new LinkedList<>();
		long its = 0;
		SCAN: for (final IStockItem stockItem : stockItems) {
			long startIteration = ThreadMonitoringController.getInstance().getTime();

			final IProduct product = stockItem.getProduct();
			__debug("\t%s, barcode %d, amount %d, incoming %d, min stock %d", product.getName(), product.getBarcode(),
					stockItem.getAmount(), stockItem.getIncomingAmount(), stockItem.getMinStock());

			final long virtualAmount = stockItem.getAmount() + stockItem.getIncomingAmount();
			if (virtualAmount >= stockItem.getMinStock()) {
				__debug("\t\tvirtual stock %d => not low stock", virtualAmount);
				continue SCAN;
			}

			result.add(stockItem);

			ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.CALC_ORDER,
					MonitoringMetadata.RESOURCE_CPU, startIteration);
			its++;
		}
		ThreadMonitoringController.getInstance().logLoopIterationCount(MonitoringMetadata.ORDER_LOOP, its);

		__debug("%d really low-stock items in store %d", result.size(), storeID);
		return result;
	}

	/**
	 * Orders by default the minimum stock items for each low running product/good.
	 * <p>
	 * Required for UC 8
	 * 
	 * @param stockItems collection of product stock items that run low
	 * @return Collection of Product/Amount tuples for each product, which
	 *         represents the required amount of each product.
	 */
	private Collection<ProductAmountTO> __calculateRequiredAmounts(long storeID,
			final Collection<IStockItem> stockItems) {
		final Collection<ProductAmountTO> result = Lists.newArrayList();

		//
		// Order at least minimum stock for each item, but do not exceed stock
		// limits. The stock of each item in the collections is guaranteed lower
		// than the minimum (including the incoming amount), so we will never
		// exceed the maximum level.
		//
		long its = 0;
		for (final IStockItem stockItem : stockItems) {
			long start = ThreadMonitoringController.getInstance().getTime();
			long orderAmount = stockItem.getMinStock();
			if (2 * stockItem.getMinStock() >= stockItem.getMaxStock()) {
				orderAmount = stockItem.getMaxStock() - stockItem.getMinStock();
			}

			final ProductAmountTO pa = new ProductAmountTO();
			pa.setProduct(enterpriseFactory.fillProductTO(stockItem.getProduct()));
			pa.setAmount(orderAmount);

			result.add(pa);
			ThreadMonitoringController.getInstance().logResponseTime(MonitoringMetadata.CALCULATE_AMOUNTS,
					MonitoringMetadata.RESOURCE_CPU, start);
		}
		ThreadMonitoringController.getInstance().logLoopIterationCount(MonitoringMetadata.CALC_PROD_AMOUNT_LOOP, its);

		__debug("%d products to be ordered by store %d", result.size(), storeID);
		return result;
	}

	/**
	 * Requests the product dispatcher to determine the stores to transfer goods
	 * from and to issue the product movement orders. Returns the amounts of items
	 * incoming from other stores.
	 * 
	 * @throws NotInDatabaseException
	 * 
	 * @throws ComponentNotAvailableException if the product dispatcher cannot be
	 *                                        found
	 * @throws RemoteException                if there is a problem communicating
	 *                                        with the dispatcher
	 */
	private ProductAmountTO[] __orderRequiredProducts(long storeID, final Collection<ProductAmountTO> requiredProducts)
			throws NotInDatabaseException {
		//
		// Connect to the product dispatcher and order the required products
		// from other stores in the enterprise. Do nothing if the connection
		// cannot be established.
		final IStore store = __storeQuery.queryStoreById(storeID);

		// TODO maybe also activate this
		// final ProductAmountTO[] result = __dispatcher
		// .dispatchProductsFromOtherStores(store.getId(),
		// requiredProducts);
		final ProductAmountTO[] result = new ProductAmountTO[0];

		__debug("%d products incoming to store %d", result.length, storeID);
		return result;
	}

	/**
	 * Registers the products coming from other stores by increasing the incoming
	 * amount of stock items corresponding to the incoming products.
	 * 
	 * @throws UpdateException
	 */
	private void __registerIncomingProducts(long storeID, final ProductAmountTO[] incomingProducts)
			throws UpdateException {
		for (final ProductAmountTO incomingProductTO : incomingProducts) {
			final ProductTO incomingProduct = incomingProductTO.getProduct();
			final IStockItem stockItem = __storeQuery.queryStockItem(storeID, incomingProduct.getBarcode());

			final long incomingAmount = incomingProductTO.getAmount();
			stockItem.setIncomingAmount(stockItem.getIncomingAmount() + incomingAmount);
			pctx.updateEntity(stockItem);

			__debug("\t%s, barcode %d, incoming amount %d", incomingProduct.getName(), incomingProduct.getBarcode(),
					incomingAmount);
		}
	}

	private static void __debug(final String format, final Object... args) {
		__log(Level.DEBUG, format, args);
	}

	private static void __warn(final String format, final Object... args) {
		__log(Level.WARN, format, args);
	}

	private static void __error(final String format, final Object... args) {
		__log(Level.ERROR, format, args);
	}

	private static void __log(final Level level, final String format, final Object... args) {
		if (__log__.isEnabledFor(level)) {
			__log__.log(level, String.format(format, args));
		}
	}

	@Override
	public ProductWithStockItemTO updateStockItem(long storeID, StockItemTO stockItemTO)
			throws NotInDatabaseException, UpdateException {
		final IStockItem si = __storeQuery.queryStockItemById(stockItemTO.getId());

		si.setSalesPrice(stockItemTO.getSalesPrice());
		si.setAmount(stockItemTO.getAmount());
		si.setMaxStock(stockItemTO.getMaxStock());
		si.setMinStock(stockItemTO.getMinStock());

		pctx.updateEntity(si);

		return storeFactory.fillProductWithStockItemTO(si);
	}

	@Override
	public void createStockItem(long storeID, ProductWithStockItemTO stockItemTO)
			throws NotInDatabaseException, CreateException {
		IStore store = __storeQuery.queryStoreById(storeID);

		IStockItem item = storeFactory.getNewStockItem();
		item.setAmount(stockItemTO.getStockItemTO().getAmount());
		item.setIncomingAmount(stockItemTO.getStockItemTO().getIncomingAmount());
		item.setMaxStock(stockItemTO.getStockItemTO().getMaxStock());
		item.setMinStock(stockItemTO.getStockItemTO().getMinStock());
		item.setSalesPrice(stockItemTO.getStockItemTO().getSalesPrice());

		IProduct product;
		if (stockItemTO.getId() != 0) {
			product = __enterpriseQuery.queryProductByID(stockItemTO.getId());
		} else {
			product = __enterpriseQuery.queryProductByBarcode(stockItemTO.getBarcode());
		}

		item.setProduct(product);
		item.setProductBarcode(product.getBarcode());
		item.setStore(store);
		item.setStoreLocation(store.getLocation());
		item.setStoreName(store.getName());

		pctx.createEntity(item);
	}

}
