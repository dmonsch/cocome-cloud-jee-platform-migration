package org.cocome.tradingsystem.inventory.data.persistence;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import dmodel.pipeline.monitoring.util.ManualMapping;

@Stateless
public class CloudPersistence implements IPersistence {

	@EJB
	private IPersistenceContext persistenceContext;
	
	@Override
	public IPersistenceContext getPersistenceContext() {
		return persistenceContext;
	}
}
