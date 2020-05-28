package org.cocome.tradingsystem.inventory.data.persistence;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import dmodel.designtime.monitoring.util.ManualMapping;

@Stateless
public class CloudPersistence implements IPersistence {

	@EJB
	private IPersistenceContext persistenceContext;
	
	@Override
	@ManualMapping("_6ZfDVB8PEdyY_su_CT9KsQ")
	public IPersistenceContext getPersistenceContext() {
		return persistenceContext;
	}
}
