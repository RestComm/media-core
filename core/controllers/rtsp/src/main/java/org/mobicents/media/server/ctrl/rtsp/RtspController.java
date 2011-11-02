/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.ctrl.rtsp;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.rtsp.RtspHeaders;
import org.jboss.netty.handler.codec.rtsp.RtspMethods;
import org.jboss.netty.handler.codec.rtsp.RtspResponseStatuses;
import org.jboss.netty.handler.codec.rtsp.RtspVersions;
import org.mobicents.media.server.ctrl.rtsp.stack.RtspListener;
import org.mobicents.media.server.ctrl.rtsp.stack.RtspServerStackImpl;
import org.mobicents.media.server.spi.NamingService;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtspController implements RtspListener {

	private static final Logger logger = Logger.getLogger(RtspController.class);

	public static final String SERVER = "Mobicents Media Server";

	private RtspServerStackImpl serverStack = null;
	private String bindAddress = "127.0.0.1";
	private int port = 554;
	private NamingService namingService;
	private String mediaDir = null;

	private static final String DATE_PATTERN = "EEE, d MMM yyyy HH:mm:ss z";
	private static final SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN);

	private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();
	
	private Set<String> endpoints;

	public String getBindAddress() {
		return this.bindAddress;
	}

	public void setBindAddress(String bindAddress) throws UnknownHostException {
		this.bindAddress = bindAddress;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getMediaDir() {
		return mediaDir;
	}

	public void setMediaDir(String mediaDir) {
		this.mediaDir = mediaDir;
	}

	public NamingService getNamingService() {
		return namingService;
	}

	public void setNamingService(NamingService namingService) {
		this.namingService = namingService;
	}

	public void create() {
		logger.info("Starting RTSP Controller module for MMS");
	}

	public void start() throws Exception {
		this.serverStack = new RtspServerStackImpl(this.bindAddress, this.port);
		this.serverStack.setRtspListener(this);
		this.serverStack.start();

		logger
				.info("Started RTSP Controller module for MMS. Bound at IP " + this.bindAddress + " at port "
						+ this.port);
	}

	public void stop() {
		logger.info("Stoping RTSP Controller module for MMS. Listening at IP " + this.bindAddress + " port "
				+ this.port);
		this.serverStack.stop();
	}

	public void destroy() {
		logger.info("Stopped RTSP Controller module for MMS");
	}

	public void onRtspRequest(HttpRequest request, Channel channel) {
		logger.info("Receive request " + request);
		Callable<HttpResponse> action = null;
		HttpResponse response = null;
		try {

			if (request.getMethod().equals(RtspMethods.OPTIONS)) {
				action = new OptionsAction(this, request);
				response = action.call();
			} else if (request.getMethod().equals(RtspMethods.DESCRIBE)) {
				action = new DescribeAction(this, request);
				response = action.call();
			} else if (request.getMethod().equals(RtspMethods.SETUP)) {
				InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
				String remoteIp = inetSocketAddress.getAddress().getHostAddress();
				action = new SetupAction(this, request, remoteIp);
				response = action.call();
			} else if (request.getMethod().equals(RtspMethods.PLAY)) {
				action = new PlayAction(this, request);
				response = action.call();
			} else if (request.getMethod().equals(RtspMethods.TEARDOWN)) {
				action = new TeardownAction(this, request);
				response = action.call();
			} else if (request.getMethod().equals(HttpMethod.GET)) {

				String date = formatter.format(new Date());

				response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK);
				response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
				response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
				response.setHeader(HttpHeaders.Names.DATE, date);
				response.setHeader(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_STORE);
				response.setHeader(HttpHeaders.Names.PRAGMA, HttpHeaders.Values.NO_CACHE);
				response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/x-rtsp-tunnelled");

			} else if (request.getMethod().equals(HttpMethod.POST)) {
				// http://developer.apple.com/quicktime/icefloe/dispatch028.html
				// The POST request is never replied to by the server.
				logger.info("POST Response = " + response);
				// TODO : Map this request to GET

				return;

				// response = new DefaultHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.NOT_IMPLEMENTED);
			} else {
				response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.METHOD_NOT_ALLOWED);
				response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
				response.setHeader(RtspHeaders.Names.CSEQ, request.getHeader(RtspHeaders.Names.CSEQ));
				response.setHeader(RtspHeaders.Names.ALLOW, OptionsAction.OPTIONS);
			}

		} catch (Exception e) {
			logger.error("Unexpected error during processing,Caused by ", e);

			response = new DefaultHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
			response.setHeader(HttpHeaders.Names.SERVER, RtspController.SERVER);
			response.setHeader(RtspHeaders.Names.CSEQ, request.getHeader(RtspHeaders.Names.CSEQ));
		}

		logger.info("Sending Response " + response.toString() + " For Request " + request.toString());
		channel.write(response);
	}

	public void onRtspResponse(HttpResponse arg0) {
		// TODO Auto-generated method stub

	}

	protected Session getSession(String sessionId) {
		return this.sessions.get(sessionId);
	}

	protected void addSession(Session session) {
		this.sessions.put(session.getId(), session);
	}

	protected void removeSession(String sessionId) {
		this.sessions.remove(sessionId);
	}

	public Set<String> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(Set<String> endpoints) {
		this.endpoints = endpoints;
	}

}
