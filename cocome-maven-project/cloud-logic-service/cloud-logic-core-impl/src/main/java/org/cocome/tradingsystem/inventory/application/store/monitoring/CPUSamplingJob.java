package org.cocome.tradingsystem.inventory.application.store.monitoring;

import org.apache.bcel.generic.CPInstruction;
import org.hyperic.sigar.Sigar;

import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.sampler.ISampler;
import kieker.monitoring.timer.ITimeSource;
import tools.vitruv.applications.pcmjava.modelrefinement.parameters.monitoring.records.ResourceUtilizationRecord;

public class CPUSamplingJob implements ISampler {
	
	private static final ITimeSource TIME_SOURCE = MonitoringController.getInstance().getTimeSource();
	
	private String containerId;
	private String sessionId;
	
	private Sigar sigar;
	
	public CPUSamplingJob(String containerId, String sessionId) {
		this.containerId = containerId;
		this.sessionId = sessionId;
		
		this.sigar = new Sigar();
	}

	@Override
	public void sample(IMonitoringController arg0) throws Exception {
		double cpuPerc = sigar.getCpuPerc().getCombined();
		
		ResourceUtilizationRecord util = new ResourceUtilizationRecord(sessionId, containerId, MonitoringMetadata.RESOURCE_CPU, cpuPerc, TIME_SOURCE.getTime());
		arg0.newMonitoringRecord(util);
	}

}
