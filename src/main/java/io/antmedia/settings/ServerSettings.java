package io.antmedia.settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.annotations.NotSaved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.catalina.util.NetMask;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ServerSettings implements ApplicationContextAware {
	
	public static final String BEAN_NAME = "ant.media.server.settings";

	
	private static final String SETTINGS_HEART_BEAT_ENABLED = "server.heartbeatEnabled";
	
	private static final String SETTINGS_USE_GLOBAL_IP = "useGlobalIp";
	
	private String allowedDashboardCIDR;

	@JsonIgnore
	@NotSaved
	private List<NetMask> allowedCIDRList = new ArrayList<>();

	
	private static Logger logger = LoggerFactory.getLogger(ServerSettings.class);

	private static String localHostAddress;
	
	private static String globalHostAddress;
	
	private static String hostAddress;
	
	/**
	 * Fully Qualified Domain Name
	 */
	private String serverName;
	/**
	 * Customer License Key
	 */
	private String licenceKey;
	
	/**
	 * The setting for customized marketplace build
	 */
	private boolean buildForMarket = false;
	
	
	
	private String logLevel = null;
	
	@Value( "${"+SETTINGS_HEART_BEAT_ENABLED+":true}" )
	private boolean heartbeatEnabled; 
	
	@Value( "${"+SETTINGS_USE_GLOBAL_IP+":false}" )
	private boolean useGlobalIp;

	public boolean isBuildForMarket() {
		return buildForMarket;
	}

	public void setBuildForMarket(boolean buildForMarket) {
		this.buildForMarket = buildForMarket;
	}

	public String getLicenceKey() {
		return licenceKey;
	}

	public void setLicenceKey(String licenceKey) {
		this.licenceKey = licenceKey;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

 	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public boolean isHeartbeatEnabled() {
		return heartbeatEnabled;
	}

	public void setHeartbeatEnabled(boolean heartbeatEnabled) {
		this.heartbeatEnabled = heartbeatEnabled;
	}
	
	public  String getHostAddress() {
		if (hostAddress == null ) {
			//which means that context is not initialized yet so that return localhost address
			logger.warn("ServerSettings is not initialized yet so that return local host address: {}", getLocalHostAddress());
			return getLocalHostAddress();
		}
		return hostAddress;
	}
	
	public static String getGlobalHostAddress(){
		
		if (globalHostAddress == null) {
			InputStream in = null;
			try {
				in = new URL("http://checkip.amazonaws.com").openStream();
				globalHostAddress = IOUtils.toString(in, Charset.defaultCharset()).trim();
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			finally {
				IOUtils.closeQuietly(in);
			}
		}

		return globalHostAddress;
	}
	
	public static String getLocalHostAddress() {

		if (localHostAddress == null) {
			long startTime = System.currentTimeMillis();
			try {
				/*
				 * InetAddress.getLocalHost().getHostAddress() takes long time(5sec in macos) to return.
				 * Let it is run once
				 */
				localHostAddress = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			long diff = System.currentTimeMillis() - startTime;
			if (diff > 1000) {
				logger.warn("Getting host adress took {}ms. it's cached now and will return immediately from now on. You can "
						+ " alternatively set serverName in conf/red5.properties file ", diff);
			}
		}


		return localHostAddress;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
	
		if (useGlobalIp) {
			hostAddress = getGlobalHostAddress();
		}
		else {
			//************************************
			//this method may sometimes takes long to return
			//delaying initialization may cause some after issues
			hostAddress = getLocalHostAddress();
		}
		
	}

	public boolean isUseGlobalIp() {
		return useGlobalIp;
	}

	public void setUseGlobalIp(boolean useGlobalIp) {
		this.useGlobalIp = useGlobalIp;
	}
	
	/**
	 * the getAllowedCIDRList and setAllowedCIDRList are synchronized because
	 * ArrayList may throw concurrent modification
	 * 
	 * @param allowedDashboardCIDR
	 */
	public void setAllowedDashboardCIDR(String allowedDashboardCIDR) {
		this.allowedDashboardCIDR = allowedDashboardCIDR;
		allowedCIDRList = new ArrayList<>();
		fillFromInput(allowedDashboardCIDR, allowedCIDRList);
	}
	
	public String getAllowedDashboardCIDR() {
		return allowedDashboardCIDR;
	}

	public List<NetMask> getAllowedCIDRList() {
		if (allowedCIDRList.isEmpty()) {
			fillFromInput(allowedDashboardCIDR, allowedCIDRList);
		}
		return allowedCIDRList;
	}

	/**
	 * Fill a {@link NetMask} list from a string input containing a comma-separated
	 * list of (hopefully valid) {@link NetMask}s.
	 *
	 * @param input  The input string
	 * @param target The list to fill
	 * @return a string list of processing errors (empty when no errors)
	 */
	private List<String> fillFromInput(final String input, final List<NetMask> target) {
		target.clear();
		if (input == null || input.isEmpty()) {
			return Collections.emptyList();
		}

		final List<String> messages = new LinkedList<>();
		NetMask nm;

		for (final String s : input.split("\\s*,\\s*")) {
			try {
				nm = new NetMask(s);
				target.add(nm);
			} catch (IllegalArgumentException e) {
				messages.add(s + ": " + e.getMessage());
			}
		}

		return Collections.unmodifiableList(messages);
	}

}
