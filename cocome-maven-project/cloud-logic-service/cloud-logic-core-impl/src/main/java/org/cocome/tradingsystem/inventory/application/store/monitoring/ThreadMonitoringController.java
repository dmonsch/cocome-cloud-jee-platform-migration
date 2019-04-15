package org.cocome.tradingsystem.inventory.application.store.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import kieker.common.configuration.Configuration;
import kieker.monitoring.core.configuration.ConfigurationFactory;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.controller.WriterController;
import kieker.monitoring.core.sampler.ScheduledSamplerJob;
import kieker.monitoring.sampler.sigar.ISigarSamplerFactory;
import kieker.monitoring.sampler.sigar.SigarSamplerFactory;
import kieker.monitoring.sampler.sigar.samplers.CPUsDetailedPercSampler;
import kieker.monitoring.timer.ITimeSource;
import kieker.monitoring.writer.filesystem.AsciiFileWriter;
import tools.vitruv.applications.pcmjava.modelrefinement.parameters.monitoring.records.BranchRecord;
import tools.vitruv.applications.pcmjava.modelrefinement.parameters.monitoring.records.LoopRecord;
import tools.vitruv.applications.pcmjava.modelrefinement.parameters.monitoring.records.ResponseTimeRecord;
import tools.vitruv.applications.pcmjava.modelrefinement.parameters.monitoring.records.ServiceCallRecord;

/**
 * This controller abstracts the creation of monitoring records and the state
 * handling.
 *
 * @author JP
 *
 */
public class ThreadMonitoringController {

	private static final int INITIAL_SERVICE_DEPTH_COUNT = 10;

	private static final ITimeSource TIME_SOURCE = MonitoringController.getInstance().getTimeSource();

	private static final String OUTPATH = "/etc/monitoring/";

	private static final ThreadLocal<ThreadMonitoringController> CONTROLLER = ThreadLocal
			.withInitial(new Supplier<ThreadMonitoringController>() {
				@Override
				public ThreadMonitoringController get() {
					return new ThreadMonitoringController(Thread.currentThread().getId(), INITIAL_SERVICE_DEPTH_COUNT);
				}
			});

	private static volatile String sessionId;

	private final long threadId;

	private static IMonitoringController monitoringController = null;

	static {
		final Configuration configuration = ConfigurationFactory.createDefaultConfiguration();
		configuration.setProperty(ConfigurationFactory.METADATA, "true");
		configuration.setProperty(ConfigurationFactory.AUTO_SET_LOGGINGTSTAMP, "true");
		configuration.setProperty(ConfigurationFactory.WRITER_CLASSNAME, AsciiFileWriter.class.getName());
		// configuration.setProperty(WriterController.RECORD_QUEUE_SIZE, "5");
		configuration.setProperty(AsciiFileWriter.CONFIG_FLUSH, "true");
		configuration.setProperty(ConfigurationFactory.TIMER_CLASSNAME, "kieker.monitoring.timer.SystemMilliTimer");
		configuration.setProperty(AsciiFileWriter.CONFIG_PATH, OUTPATH);

		monitoringController = MonitoringController.createInstance(configuration);
		
		ThreadMonitoringController.getInstance().registerCpuSampler(MonitoringMetadata.CONTAINER_STORE_ID, "<none>");
	}

	/**
	 * Stack and cache of service monitoring controllers. Already initialized
	 * controllers are reused.
	 */
	private final List<ServiceMonitoringController> serviceControllers;

	private int currentServiceIndex;

	private ServiceMonitoringController currentServiceController;

	private long overhead;

	private boolean cpuSamplerActive;
	private ScheduledSamplerJob samplerJob;

	private ThreadMonitoringController(final long threadId, final int initialServiceDepthCount) {
		this.threadId = threadId;
		this.serviceControllers = new ArrayList<>(initialServiceDepthCount);
		for (int i = 0; i < initialServiceDepthCount; i++) {
			this.serviceControllers.add(new ServiceMonitoringController());
		}
		this.currentServiceIndex = -1;
		this.currentServiceController = null;
		this.overhead = 0;
		this.cpuSamplerActive = false;
	}

	public void registerCpuSampler(String containerId, String sessionId) {
		if (!cpuSamplerActive) {
			CPUSamplingJob job = new CPUSamplingJob(containerId, sessionId);

			samplerJob = monitoringController.schedulePeriodicSampler(job, 0, 100, TimeUnit.MILLISECONDS);
			cpuSamplerActive = true;
		}
	}
	
	public void unregisterCpuSampler() {
		if (cpuSamplerActive) {
			monitoringController.removeScheduledSampler(samplerJob);
			cpuSamplerActive = false;
		}
	}

	/**
	 * Calls this method after entering the service.
	 * {@link ThreadMonitoringController#exitService()} must be called before
	 * exiting the service. Surround the inner code with try finally and call
	 * {@link ThreadMonitoringController#exitService()} inside the finally block .
	 *
	 * @param serviceId
	 *            The SEFF Id for the service.
	 */
	public void enterService(final String serviceId, final String assemblyId) {
		long before = System.currentTimeMillis();
		this.enterService(serviceId, assemblyId, ServiceParameters.EMPTY);
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Calls this method after entering the service.
	 * {@link ThreadMonitoringController#exitService()} must be called before
	 * exiting the service. Surround the inner code with try finally and call
	 * {@link ThreadMonitoringController#exitService()} inside the finally block .
	 *
	 * @param serviceId
	 *            The SEFF Id for the service.
	 * @param serviceParameters
	 *            The service parameter values.
	 */
	public void enterService(final String serviceId, final String assemblyId, final ServiceParameters serviceParameters) {
		long before = System.currentTimeMillis();
		String currentServiceExecutionId = null;
		String currentCallerId = null;
		if (this.currentServiceController != null) {
			currentServiceExecutionId = this.currentServiceController.getServiceExecutionId();
			currentCallerId = this.currentServiceController.getCurrentCallerId();
		}

		this.currentServiceIndex += 1;
		ServiceMonitoringController newService;
		if (this.currentServiceIndex >= this.serviceControllers.size()) {
			newService = new ServiceMonitoringController();
			this.serviceControllers.add(new ServiceMonitoringController());
		} else {
			newService = this.serviceControllers.get(this.currentServiceIndex);
		}
		
		newService.enterService(serviceId, assemblyId, this.threadId, sessionId, serviceParameters, currentCallerId,
				currentServiceExecutionId);

		this.currentServiceController = newService;
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Leaves the current service and writes the monitoring record.
	 * {@link ThreadMonitoringController#enterService(String)} must be called first.
	 */
	public void exitService() {
		long before = System.currentTimeMillis();
		this.currentServiceController.exitService();
		this.currentServiceIndex -= 1;
		if (this.currentServiceIndex >= 0) {
			this.currentServiceController = this.serviceControllers.get(this.currentServiceIndex);
		} else {
			this.currentServiceController = null;
		}
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Gets the current time of the time source for monitoring records. Use this
	 * method to get the start time for response time records.
	 *
	 * @return The current time.
	 */
	public long getTime() {
		return TIME_SOURCE.getTime();
	}

	/**
	 * Writes a branch monitoring record.
	 *
	 * @param branchId
	 *            The abstract action id of the branch.
	 * @param executedBranchId
	 *            The abstract action id of the executed branch transition.
	 */
	public void logBranchExecution(final String branchId, final String executedBranchId) {
		long before = System.currentTimeMillis();
		this.currentServiceController.logBranchExecution(branchId, executedBranchId);
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Writes a loop monitoring record.
	 *
	 * @param loopId
	 *            The abstract action id of the loop.
	 * @param loopIterationCount
	 *            The executed iterations of the loop.
	 */
	public void logLoopIterationCount(final String loopId, final long loopIterationCount) {
		long before = System.currentTimeMillis();
		this.currentServiceController.logLoopIterationCount(loopId, loopIterationCount);
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Writes a response time monitoring record. The stop time of the response time
	 * is taken by this method's internals.
	 *
	 * @param internalActionId
	 *            The abstract action id of the internal action.
	 * @param resourceId
	 *            The id of the resource type.
	 * @param startTime
	 *            The start time of the response time.
	 */
	public void logResponseTime(final String internalActionId, final String resourceId, final long startTime) {
		long before = System.currentTimeMillis();
		this.currentServiceController.logResponseTime(internalActionId, resourceId, startTime);
		overhead += System.currentTimeMillis() - before;
	}

	/**
	 * Sets the abstract action id of the next external call.
	 *
	 * @param currentCallerId
	 *            The abstract action id of the next external call.
	 */
	public void setCurrentCallerId(final String currentCallerId) {
		long before = System.currentTimeMillis();
		this.currentServiceController.setCurrentCallerId(currentCallerId);
		overhead += System.currentTimeMillis() - before;
	}

	public long getOverhead() {
		return overhead;
	}

	/**
	 * Gets the singleton instance. Each thread has its own instance.
	 *
	 * @return Instance of {@link ThreadMonitoringController}.
	 */
	public static ThreadMonitoringController getInstance() {
		return CONTROLLER.get();
	}

	/**
	 * Gets the current session id.
	 *
	 * @return The current session ids.
	 */
	public static String getSessionId() {
		return sessionId;
	}

	/**
	 * Sets the session id.
	 *
	 * @param id
	 *            The new session id.
	 */
	public static void setSessionId(final String id) {
		sessionId = id;
	}

	private static class ServiceMonitoringController {

		private long serviceStartTime;

		private String serviceId;
		private ServiceParameters serviceParameters;
		private String serviceExecutionId;
		private String sessionId;
		private String callerServiceExecutionId;
		private String callerId;
		private String currentCallerId;
		private String assemblyId;

		public void enterService(final String serviceId, final String assemblyId, final long threadId, final String sessionId,
				final ServiceParameters serviceParameters, final String callerId,
				final String callerServiceExecutionId) {
			this.serviceId = serviceId;
			this.sessionId = sessionId;
			this.serviceParameters = serviceParameters;
			this.callerServiceExecutionId = callerServiceExecutionId;
			this.callerId = callerId;
			this.serviceStartTime = TIME_SOURCE.getTime();
			this.serviceExecutionId = UUID.randomUUID().toString();
			this.currentCallerId = null;
			this.assemblyId = assemblyId;
		}

		public void exitService() {
			final long stopTime = TIME_SOURCE.getTime();

			ServiceCallRecord e = new ServiceCallRecord(this.sessionId, this.serviceExecutionId, this.serviceId,
					this.serviceParameters.toString(), this.callerServiceExecutionId, this.callerId, this.assemblyId,
					this.serviceStartTime, stopTime);

			monitoringController.newMonitoringRecord(e);
		}

		public String getCurrentCallerId() {
			return this.currentCallerId;
		}

		public String getServiceExecutionId() {
			return this.serviceExecutionId;
		}

		public void logBranchExecution(final String branchId, final String executedBranchId) {
			BranchRecord record = new BranchRecord(this.sessionId, this.serviceExecutionId, branchId, executedBranchId);

			monitoringController.newMonitoringRecord(record);
		}

		public void logLoopIterationCount(final String loopId, final long loopIterationCount) {
			LoopRecord record = new LoopRecord(this.sessionId, this.serviceExecutionId, loopId, loopIterationCount);

			monitoringController.newMonitoringRecord(record);
		}

		public void logResponseTime(final String internalActionId, final String resourceId, final long startTime) {
			long currentTime = TIME_SOURCE.getTime();

			ResponseTimeRecord record = new ResponseTimeRecord(this.sessionId, this.serviceExecutionId,
					internalActionId, resourceId, startTime, currentTime);

			monitoringController.newMonitoringRecord(record);
		}

		public void setCurrentCallerId(final String currentCallerId) {
			this.currentCallerId = currentCallerId;
		}
	}

	public void resetOverhead() {
		System.out.println("Reset");
		this.overhead = 0;
	}
}
