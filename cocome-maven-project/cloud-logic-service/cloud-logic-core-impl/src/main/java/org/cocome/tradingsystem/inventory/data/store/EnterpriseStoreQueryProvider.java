package org.cocome.tradingsystem.inventory.data.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.cocome.tradingsystem.inventory.data.enterprise.IProduct;
import org.cocome.tradingsystem.remote.access.connection.IBackendQuery;
import org.cocome.tradingsystem.remote.access.connection.QueryParameterEncoder;
import org.cocome.tradingsystem.remote.access.parsing.IBackendConversionHelper;
import org.cocome.tradingsystem.util.exception.NotInDatabaseException;

import cipm.consistency.bridge.monitoring.controller.MonitoringMetadata;
import cipm.consistency.bridge.monitoring.controller.ServiceParameters;
import cipm.consistency.bridge.monitoring.controller.ThreadMonitoringController;
import cipm.consistency.bridge.monitoring.meta.CocomeMonitoringMetadata;
import dmodel.designtime.monitoring.util.ManualMapping;


/**
 * The objects returned will only have their basic datatype attributes filled.
 * 
 * @author Tobias Pöppke
 *
 */
@Stateless
@Local(IStoreQuery.class)
public class EnterpriseStoreQueryProvider implements IStoreQuery {
	
	// TODO either cache the retrieved objects or provide faster queries which
	// return objects with only the simple attribute types set and other queries which
	// query all attributes of the objects
	
	private static final Logger LOG = Logger.getLogger(EnterpriseStoreQueryProvider.class);
	
	@Inject
	IBackendQuery backendConnection;
	
	@Inject
	IBackendConversionHelper csvHelper;
	
	@Override
	public IStore queryStore(String name, String location) {
		name = QueryParameterEncoder.encodeQueryString(name);
		location = QueryParameterEncoder.encodeQueryString(location);
		String locationQuery = "*";
		
		if (!location.equals("")) {
			locationQuery = location;
		}
		
		List<IStore> stores = (List<IStore>) csvHelper.getStores(
				backendConnection.getStores("name=LIKE%20'" + name + "';Store.location=LIKE%20'" + locationQuery + "'"));
		
		if (stores.size() > 1) {
			LOG.warn("More than one store with name " + name + 
					" and location " + location + " was found!");
		}

		try {
			return stores.get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	@ManualMapping("queryStoreById")
	public IStore queryStoreById(long storeId) throws NotInDatabaseException {
		ThreadMonitoringController.getInstance().enterService(CocomeMonitoringMetadata.SERVICE_QUERY_STORE_BY_ID, this);
		long start = ThreadMonitoringController.getInstance().getTime();
		try {
			// @START INTERNAL_ACTION{_zJrNENLxEduQ7qbNANXHPw}
			IStore store = csvHelper.getStores(
					backendConnection.getStores("id==" + storeId)).iterator().next();		
			// @END INTERNAL_ACTION{_zJrNENLxEduQ7qbNANXHPw}
			return store;
		} catch (NoSuchElementException e) {
			throw new NotInDatabaseException(
					"Store with ID " + storeId + " could not be found!");
		}  finally {
			ThreadMonitoringController.getInstance().exitInternalAction(CocomeMonitoringMetadata.INTERNAL_QUERY_STORE_BY_ID,
					MonitoringMetadata.RESOURCE_CPU, start);
			ThreadMonitoringController.getInstance().exitService(CocomeMonitoringMetadata.SERVICE_QUERY_STORE_BY_ID);
		}
	}

	// TODO don't call this method, because there is no way to retrieve the stock item id
	@Override
	@ManualMapping("queryStockItemById")
	public IStockItem queryStockItemById(long stockItemId) throws NotInDatabaseException {
		ThreadMonitoringController.getInstance().enterService(CocomeMonitoringMetadata.SERVICE_QUERY_STOCK_ITEM_BY_ID, this);
		long startTime = ThreadMonitoringController.getInstance().getTime();
		
		try {
			IStockItem item = csvHelper.getStockItems(
					backendConnection.getStockItems("id==" + stockItemId)).iterator().next();
			return item;
		} catch  (NoSuchElementException e) {
			throw new NotInDatabaseException("StockItem with ID " 
					+ stockItemId + " could not be found!");
		}  finally {
			ThreadMonitoringController.getInstance().exitInternalAction(CocomeMonitoringMetadata.INTERNAL_QUERY_STOCK_ITEM_BY_ID,
					MonitoringMetadata.RESOURCE_CPU, startTime);
			ThreadMonitoringController.getInstance().exitService(CocomeMonitoringMetadata.SERVICE_QUERY_STOCK_ITEM_BY_ID);
		}
	}

	@Override
	@ManualMapping("_D4QOch_6Edy5k9ER1TBmjg")
	public IProduct queryProductById(long productId) throws NotInDatabaseException {
		IProduct product = null;
		try {
			product = csvHelper.getProducts(
					backendConnection.getProducts("id==" + productId)).iterator().next();
			product.setId(productId);
			return product;
		} catch (NoSuchElementException e) {
			throw new NotInDatabaseException("Product with ID " + productId + " could not be found!");
		}
	}

	@Override
	public IProduct queryProductByBarcode(long barcode) {
		IProduct product = null;
		try {
			product = csvHelper.getProducts(
					backendConnection.getProducts("barcode==" + barcode)).iterator().next();
		} catch (NoSuchElementException e) {
			// Do nothing, no product found
		}
		return product;
	}

	@Override
	@ManualMapping("_x6oLQh_5Edy5k9ER1TBmjg")
	public IProductOrder queryOrderById(long orderId) throws NotInDatabaseException {
		IProductOrder productOrder;
		try {
			productOrder = csvHelper.getProductOrders(
					backendConnection.getProductOrder("id==" + orderId)).iterator().next();
		} catch (NoSuchElementException e) {
			throw new NotInDatabaseException("Order with ID " + orderId + " could not be found!");
		}
		return productOrder;
	}

	@Override
	@ManualMapping("_GqStsh_5Edy5k9ER1TBmjg")
	public Collection<IProduct> queryProducts(long storeId) {
		Collection<IProduct> products = new LinkedList<IProduct>();
		for (IStockItem item : queryAllStockItems(storeId)) {
			products = csvHelper.getProducts(
					backendConnection.getProducts("barcode==" + item.getProductBarcode()));
		}
		return products;
	}

	@Override
	public Collection<IProductOrder> queryOutstandingOrders(long storeId) {
		Collection<IProductOrder> productOrders = csvHelper.getProductOrders(
				backendConnection.getProductOrder("store.id==" + storeId + ";ProductOrder.deliveryDate=<e.orderingDate"));
		return productOrders;
	}

	@Override
	@ManualMapping("_obZ3wh_5Edy5k9ER1TBmjg")
	public Collection<IStockItem> queryAllStockItems(long storeId) {
		Collection<IStockItem> stockItems = csvHelper.getStockItems(
				backendConnection.getStockItems("store.id==" + storeId));
		return stockItems;
	}

	@Override
	@ManualMapping("_ZU7A0h_5Edy5k9ER1TBmjg")
	public Collection<IStockItem> queryLowStockItems(long storeId) {
		ServiceParameters paras = new ServiceParameters();
		ThreadMonitoringController.getInstance().enterService(CocomeMonitoringMetadata.SERVICE_QUERY_LOW_STOCK_ITEMS, this, paras);
		long startTime = ThreadMonitoringController.getInstance().getTime();
		// Hacky way to get the result. We have to use e.minStock as comparison because
		// using StockItem.minStock will not be parsed and the query will return an error
		Collection<IStockItem> stockItems = csvHelper.getStockItems(
				backendConnection.getStockItems("store.id==" + storeId + ";StockItem.amount=<e.minStock"));
		
		ThreadMonitoringController.getInstance().exitInternalAction(CocomeMonitoringMetadata.INTERNAL_QUERY_LOW_STOCK_ITEMS,
				MonitoringMetadata.RESOURCE_CPU, startTime);
		ThreadMonitoringController.getInstance().exitService(CocomeMonitoringMetadata.SERVICE_QUERY_LOW_STOCK_ITEMS);
		
		return stockItems;
	}

	@Override
	@ManualMapping("queryStockItem")
	public IStockItem queryStockItem(long storeId, long productBarcode) {
		ServiceParameters paras = new ServiceParameters();
		ThreadMonitoringController.getInstance().enterService(CocomeMonitoringMetadata.SERVICE_QUERY_STOCK_ITEM, this, paras);
		// @START INTERNAL_ACTION{_2GlXMNL-EdujoZKiiOMQBA}
		
		long startTime = ThreadMonitoringController.getInstance().getTime();
		IStockItem item = null;
		try {
			item = csvHelper.getStockItems(
					backendConnection.getStockItems("product.barcode==" + productBarcode 
					+ ";StockItem.store.id==" + storeId)).iterator().next();
		} catch (NoSuchElementException e) {
			// Do nothing, just return null and don't crash
		}
		// @END INTERNAL_ACTION{_2GlXMNL-EdujoZKiiOMQBA}
		
		ThreadMonitoringController.getInstance().exitInternalAction(CocomeMonitoringMetadata.INTERNAL_QUERY_STOCK_ITEM,
				MonitoringMetadata.RESOURCE_CPU, startTime);
		ThreadMonitoringController.getInstance().exitService(CocomeMonitoringMetadata.SERVICE_QUERY_STOCK_ITEM);
		
		return item;
	}

	@Override
	public Collection<IStockItem> queryStockItemsByProductId(long storeId,
			long[] productIds) {
		List<IStockItem> items = new LinkedList<IStockItem>();
		for (long productId : productIds) {
			Collection<IStockItem> stockItems = csvHelper.getStockItems(
					backendConnection.getStockItems("store.id==" + storeId + ";product.id==" + productId));
			items.addAll(stockItems);
		}
		return items;
	}

	@Override
	public IProductOrder queryProductOrder(long storeId,
			long productBarcode, long amount) throws NotInDatabaseException {
		Collection<IProductOrder> productOrders = csvHelper.getProductOrders(
				backendConnection.getProductOrder("amount==" + amount
						+ "store.id==" + storeId 
						+ ";product.barcode==" + productBarcode));
		try {
			return productOrders.iterator().next();
		} catch (NoSuchElementException e) {
			throw new NotInDatabaseException("The product order for product barcode " + productBarcode 
					+ " with amount " + amount + " could not be found in store " + storeId);
		}
	}

	@Override
	public Collection<IProductOrder> queryAllOrders(long storeId) {
		Collection<IProductOrder> productOrders = csvHelper.getProductOrders(
				backendConnection.getProductOrder("store.id==" + storeId));
		return productOrders;
	}
}
