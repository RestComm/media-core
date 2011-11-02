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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * 
 * @author amit.bhayani
 * 
 */
public class RtspServerStackImpl implements RtspStack {

	private static Logger logger = Logger.getLogger(RtspServerStackImpl.class);

	private final String address;
	private final int port;
	private final InetAddress inetAddress;
	private Channel channel = null;
	private ServerBootstrap bootstrap = null;

	private RtspListener listener = null;

	static final ChannelGroup allChannels = new DefaultChannelGroup(
			"mms-server");

	public RtspServerStackImpl(String address, int port)
			throws UnknownHostException {
		this.address = address;
		this.port = port;
		inetAddress = InetAddress.getByName(this.address);
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

		bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors
								.newCachedThreadPool(new RtspServerBossThreadFactory()),
						Executors
								.newCachedThreadPool(new RtspServerWorkerThreadFactory())));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new RtspServerPipelineFactory(this));

		// Bind and start to accept incoming connections.
		channel = bootstrap.bind(bindAddress);
		allChannels.add(channel);

		logger.info("Mobicents RTSP Server started and bound to "
				+ bindAddress.toString());

	}

	public void stop() {

		ChannelGroupFuture future = allChannels.close();
		future.awaitUninterruptibly();
		bootstrap.getFactory().releaseExternalResources();

	}

	public void setRtspListener(RtspListener listener) {
		this.listener = listener;

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

	private class ServerChannelFutureListener implements ChannelFutureListener {

		public void operationComplete(ChannelFuture arg0) throws Exception {
			logger.info("Mobicents RTSP Server Stop complete");
		}

	}

	public void sendRquest(HttpRequest rtspRequest, String host, int port) {
		throw new UnsupportedOperationException("Not Supported yet");
	}
}

class RtspServerBossThreadFactory implements ThreadFactory {

	public static final AtomicLong sequence = new AtomicLong(0);
	private ThreadGroup factoryThreadGroup = new ThreadGroup(
			"RtspServerBossThreadGroup[" + sequence.incrementAndGet() + "]");

	public Thread newThread(Runnable r) {
		Thread t = new Thread(this.factoryThreadGroup, r);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}

class RtspServerWorkerThreadFactory implements ThreadFactory {

	public static final AtomicLong sequence = new AtomicLong(0);
	private ThreadGroup factoryThreadGroup = new ThreadGroup(
			"RtspServerWorkerThreadGroup[" + sequence.incrementAndGet() + "]");

	public Thread newThread(Runnable r) {
		Thread t = new Thread(this.factoryThreadGroup, r);
		t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}
