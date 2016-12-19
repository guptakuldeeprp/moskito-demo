package com.zycus.monitoring.moskito.connector;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.anotheria.anoplass.api.API;
import net.anotheria.moskito.webui.accumulators.api.AccumulatorAPI;
import net.anotheria.moskito.webui.dashboards.api.DashboardAPI;
import net.anotheria.moskito.webui.gauges.api.GaugeAPI;
import net.anotheria.moskito.webui.journey.api.JourneyAPI;
import net.anotheria.moskito.webui.producers.api.ProducerAPI;
import net.anotheria.moskito.webui.shared.api.AdditionalFunctionalityAPI;
import net.anotheria.moskito.webui.threads.api.ThreadAPI;
import net.anotheria.moskito.webui.threshold.api.ThresholdAPI;
import net.anotheria.moskito.webui.util.RemoteInstance;

import org.distributeme.core.ServiceDescriptor;

public class MoskitoConnector {

	/**
	 * A cache for all remote instances.
	 */
	// TODO: Figure out if we can do fine without cache.
	private static ConcurrentMap<RemoteInstance, ConcurrentMap<Class<? extends API>, API>> remotes = new ConcurrentHashMap<RemoteInstance, ConcurrentMap<Class<? extends API>, API>>();

	public static JourneyAPI getJourneyAPI(RemoteInstance ri) {
		return findRemote(ri, JourneyAPI.class);
	}

	public static ThresholdAPI getThresholdAPI(RemoteInstance ri) {
		return findRemote(ri, ThresholdAPI.class);
	}

	public static AccumulatorAPI getAccumulatorAPI(RemoteInstance ri) {
		return findRemote(ri, AccumulatorAPI.class);
	}

	public static ThreadAPI getThreadAPI(RemoteInstance ri) {
		return findRemote(ri, ThreadAPI.class);
	}

	public static ProducerAPI getProducerAPI(RemoteInstance ri) {
		return findRemote(ri, ProducerAPI.class);
	}

	public static AdditionalFunctionalityAPI getAdditionalFunctionalityAPI(RemoteInstance ri) {
		return findRemote(ri, AdditionalFunctionalityAPI.class);
	}

	public static GaugeAPI getGaugeAPI(RemoteInstance ri) {

		return findRemote(ri, GaugeAPI.class);
	}

	public static DashboardAPI getDashboardAPI(RemoteInstance ri) {
		return findRemote(ri, DashboardAPI.class);
	}

	private static <T extends API> T findRemote(RemoteInstance ri, Class<T> targetClass) {
		String serviceId = null;
		try {
			Class constantsClass = Class.forName(targetClass.getPackage().getName() + ".generated."
					+ targetClass.getSimpleName() + "Constants");
			
			System.out.println("Loading constants class " + constantsClass.getName());
			Method m = constantsClass.getMethod("getServiceId");
			serviceId = (String) m.invoke(null);
			System.out.println("serviceId is " + serviceId);
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Can not find supporting classes for " + targetClass + ' ' + e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new AssertionError("Can not find supporting classes or methods for " + targetClass + ' '
					+ e.getMessage());
		} catch (InvocationTargetException e) {
			throw new AssertionError("Can not obtain service id " + targetClass + ' ' + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new AssertionError("Can not obtain service id " + targetClass + ' ' + e.getMessage());
		}

		Class<? extends T> remoteStubClass = null;
		try {
			remoteStubClass = (Class<? extends T>) Class.forName(targetClass.getPackage().getName()
					+ ".generated.Remote" + targetClass.getSimpleName() + "Stub");
		} catch (ClassNotFoundException e) {
			throw new AssertionError("Can not find supporting classes for " + targetClass);
		}
		
		System.out.println("Loaded remote stub class " + remoteStubClass.getName());
		return findRemote(ri, targetClass, remoteStubClass, serviceId);
	}

	private static <T extends API> T findRemote(RemoteInstance ri, Class<T> targetClass,
			Class<? extends T> remoteStubClass, String serviceId) {
		ConcurrentMap<Class<? extends API>, API> stubsByInterface = remotes.get(ri);
		if (stubsByInterface == null) {
			ConcurrentHashMap<Class<? extends API>, API> newStubsByInterface = new ConcurrentHashMap<Class<? extends API>, API>(
					0);
			ConcurrentMap<Class<? extends API>, API> old = remotes.putIfAbsent(ri, newStubsByInterface);
			stubsByInterface = old == null ? newStubsByInterface : old;
		}

		T stub = (T) stubsByInterface.get(targetClass);
		if (stub != null) {
			return stub;
		}
		
		// ok, we didn't have an object, we have to create it.
		ServiceDescriptor sd = new ServiceDescriptor(ServiceDescriptor.Protocol.RMI, serviceId, "any", ri.getHost(),
				ri.getPort());
		System.out.println("ServiceDescriptor: " + sd);
		try {
			Constructor<? extends T> c = remoteStubClass.getConstructor(ServiceDescriptor.class);
			T newAPI = c.newInstance(sd);
			System.out.println("newAPI: " + newAPI.getClass().getName());
			stubsByInterface.putIfAbsent(targetClass, newAPI);
			return newAPI;
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Constructor with ServiceDescriptor parameter not found in remote stub", e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(
					"Cannot connect to " + ri + ", due: " + e.getTargetException().getMessage() + ". Server at "
							+ ri.getHost() + ", port: " + ri.getPort() + " is down or not properly configured", e);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Cannot connect to " + ri + ", due: " + e.getMessage() + ". Server at "
					+ ri.getHost() + ", port: " + ri.getPort() + " is down or not properly configured", e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot connect to " + ri + ", due: " + e.getMessage() + ". Server at "
					+ ri.getHost() + ", port: " + ri.getPort() + " is down or not properly configured", e);
		}
	}

}
