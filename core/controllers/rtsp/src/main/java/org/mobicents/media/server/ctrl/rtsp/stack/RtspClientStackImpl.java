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
package org.mobicents.media.server.ctrl.rtsp.stack;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * 
 * @author amit.bhayani
 * 
 */
public class RtspClientStackImpl implements RtspStack {

	private static Logger logger = Logger.getLogger(RtspClientStackImpl.class);

	private final String address;
	private final int port;
	private final InetAddress inetAddress;
	private Channel channel = null;
	private ClientBootstrap bootstrap;

	private RtspListener listener = null;

	public RtspClientStackImpl(String address, int port)
			throws UnknownHostException {
		this.address = address;
		this.port = port;
		this.inetAddress = InetAddress.getByName(this.address);

	}

	public String getAddress() {
		return this.address;
	}

	public int getPort() {
		return this.port;
	}

	public void start() {

		InetSocketAddress bindAddress = new InetSocketAddress(this.inetAddress,
				this.port);

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors
						.newCachedThreadPool()));

		bootstrap.setOption("localAddress", bindAddress);

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new RtspClientPipelineFactory(this));

		logger.info("Mobicents RTSP Client started and bound to "
				+ bindAddress.toString());

	}

	protected void processRtspResponse(HttpResponse rtspResponse) {
		synchronized (this.listener) {
			listener.onRtspResponse(rtspResponse);
		}
	}

	protected void processRtspRequest(HttpRequest rtspRequest, Channel channel) {
		synchronized (this.listener) {
			listener.onRtspRequest(rtspRequest, channel);
		}
	}

	public void stop() {
		ChannelFuture cf = channel.getCloseFuture();
		cf.addListener(new ClientChannelFutureListener());

		channel.close();
		cf.awaitUninterruptibly();
		bootstrap.getFactory().releaseExternalResources();

	}

	public void setRtspListener(RtspListener listener) {
		this.listener = listener;

	}

	private class ClientChannelFutureListener implements ChannelFutureListener {

		public void operationComplete(ChannelFuture arg0) throws Exception {
			logger.info("Mobicents RTSP Client Stop complete");
		}

	}

	public void sendRquest(HttpRequest rtspRequest, String host, int port) {

		ChannelFuture future = null;
		if (channel == null || (channel != null && !channel.isConnected())) {
			// Start the connection attempt.
			future = bootstrap.connect(new InetSocketAddress(host, port));
		}

		// Wait until the connection attempt succeeds or fails.
		channel = future.awaitUninterruptibly().getChannel();
		if (!future.isSuccess()) {
			future.getCause().printStackTrace();
			// bootstrap.releaseExternalResources();
			return;
		}

		channel.write(rtspRequest);
	}

}
