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
package org.mobicents.media.server.impl.rtp;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package org.mobicents.media.server.impl.rtp;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author kulikov
 * 
 * 
 */
public class TestNio {

	// Pool for signalling
	private static transient ExecutorService pool = Executors.newFixedThreadPool(10, new ThreadFactoryImpl());

	private static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
	private Thread worker;
	private volatile boolean started = false;
	private static String mcAddress = "127.0.0.1";

	public static void main(String[] args) throws Exception {
		TestNio t = new TestNio();
		t.doTest();
	}

	public void doTest() throws Exception {
		int N = 250;

		started = true;
		Receiver r = new Receiver();

		Server1[] servers = new Server1[N];
		for (int i = 0; i < N; i++) {
			servers[i] = new Server1(i);
			r.add(servers[i]);
		}

		// (new Thread(new Signal())).start();

		System.out.println("Servers are ready ");

		worker = new Thread(r);
		worker.start();

		Client1[] clients = new Client1[N];
		for (int i = 0; i < N; i++) {
			clients[i] = new Client1(i);
			timer.scheduleAtFixedRate(clients[i], 0, 20, TimeUnit.MILLISECONDS);
		}

		Thread.currentThread().sleep(1000 * 60 * 1);

		timer.shutdown();
		started = false;

		pool.shutdown();

		for (int i = 0; i < N; i++) {
			servers[i].stop();
		}

		clients[50].printTicks();
		System.out.println("===============================");
		servers[50].printTicks();
	}

	private class Client implements Runnable {

		private DatagramSocket socket;
		private InetSocketAddress destination;
		private ArrayList<Long> ticks = new ArrayList(5000);

		public Client(int index) throws SocketException {
			int port = 8000 + index;
			InetSocketAddress address = new InetSocketAddress(mcAddress, port);
			destination = new InetSocketAddress(mcAddress, port - 2000);
			socket = new DatagramSocket(address);
		}

		public void run() {
			byte[] buffer = new byte[160];
			try {
				DatagramPacket p = new DatagramPacket(buffer, buffer.length, destination);
				for (int i = 0; i < 160; i++) {
					buffer[i] = (byte) (100 * 2 + 20 / 10 + 40 / 2 + 20 * 10);
				}
				socket.send(p);
				ticks.add(System.currentTimeMillis());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void printTicks() {
			System.out.println("Packets " + ticks.size());
			for (int i = 1; i < ticks.size(); i++) {
				System.out.println("Client diff for index = " + i + " =" + (ticks.get(i) - ticks.get(i - 1)));
			}
		}
	}

	private class Client1 implements Runnable {

		private DatagramChannel channel;
		private ByteBuffer buffer = ByteBuffer.allocate(160);
		private ArrayList<Long> ticks = new ArrayList(5000);

		private InetSocketAddress destination;

		public Client1(int index) throws SocketException, java.io.IOException {

			int port = 8000 + index;
			InetSocketAddress address = new InetSocketAddress(mcAddress, port);
			destination = new InetSocketAddress(mcAddress, port - 2000);
			channel = DatagramChannel.open();

			channel.socket().bind(address);

			channel.connect(destination);
			channel.configureBlocking(false);
		}

		public void run() {
			int len = 160;
			byte[] buffer = new byte[len];
			try {

				for (int i = 0; i < len; i++) {
					buffer[i] = (byte) (100 * 2 + 20 / 10 + 40 / 2 + 20 * 10);
				}
				ByteBuffer buffer1 = ByteBuffer.wrap(buffer);
				int count = 0;

				// In loop to take care of async send operation
				while (count < len) {
					count = channel.send(buffer1, destination);
					if (count != 160) {
						System.out.println("BAD!BAD!BAD! " + count);
					}
					count += count;
					buffer1.compact();
					buffer1.flip();
				}

				ticks.add(System.currentTimeMillis());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void printTicks() {
			System.out.println("Packets " + ticks.size());
			for (int i = 1; i < ticks.size(); i++) {
				System.out.println("Client diff for index = " + i + " =" + (ticks.get(i) - ticks.get(i - 1)));
			}
		}
	}

	private class Server implements Runnable {

		private DatagramSocket socket;
		private boolean stopped = false;
		private ArrayList<Long> ticks = new ArrayList(5000);
		int port = 6000;

		public Server(int index) throws SocketException {
			port = port + index;
			InetSocketAddress address = new InetSocketAddress(mcAddress, port);
			socket = new DatagramSocket(address);
			new Thread(this).start();
		}

		public void stop() {
			stopped = true;
			socket.close();
		}

		public void run() {
			byte[] buffer = new byte[1000];
			DatagramPacket packet = new DatagramPacket(buffer, 1000);
			System.out.println("Started server at port = " + port);
			while (!stopped) {
				try {
					socket.receive(packet);
					ticks.add(System.currentTimeMillis());
				} catch (IOException e) {
				}
			}
		}

		public void printTicks() {
			// for (int i = 1; i < ticks.size(); i++) {
			// System.out.println("diff =" + (ticks.get(i) - ticks.get(i - 1)));
			// }
			System.out.println("Packets " + ticks.size());
			long diff;
			long jitter = 0;
			for (int i = 1; i < ticks.size(); i++) {
				diff = (ticks.get(i) - ticks.get(i - 1));
				System.out.println("Server diff for index = " + i + " = " + diff);
				jitter = jitter + (diff - 20);
			}
			System.out.println("Jitter avg = " + jitter);
		}
	}

	private class Receiver implements Runnable {

		private ArrayList<Server1> list = new ArrayList();

		public void add(Server1 s) {
			list.add(s);
		}

		public void run() {
			System.out.println("Worker started: " + list.size() + " started = " + started);
			while (started) {
				for (Server1 receiver : list) {
					receiver.run();
				}
				try {
					Thread.currentThread().sleep(20);
				} catch (InterruptedException e) {

				}
			}
			System.out.println("Worker terminated");
		}
	}

	private class Signal implements Runnable {
		int newSignalThreads = 3;
		private int index = 0;

		public void run() {

			while (started) {

				for (int i = 0; i < newSignalThreads; i++) {

					Runnable task = new Runnable() {
						public void run() {
							index = index + 1;
							try {
								double randNumber = Math.random();
								double sqrt = Math.sqrt(randNumber);
								System.out.println("Signalling done for index = " + index + " randNumber = "
										+ randNumber + " sqrt = " + sqrt);
							} catch (Exception e) {
							}
						}
					};
					pool.submit(task);
				}// end of for loop

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}// end of whil loop
			System.out.println("Signal Thread terminated");
		}
	}

	private class Server1 implements Runnable {

		private DatagramChannel channel;
		private ByteBuffer buffer = ByteBuffer.allocate(1000);
		private ArrayList<Long> ticks = new ArrayList(5000);
		private Selector selector;

		public Server1(int index) throws SocketException, IOException {
			int port = 6000 + index;
			InetSocketAddress address = new InetSocketAddress(mcAddress, port);
			channel = DatagramChannel.open();
			System.out.println("Channel is open " + index);

			channel.socket().bind(address);
			System.out.println("Socket is bound to " + address);

			channel.connect(new InetSocketAddress(mcAddress, port + 2000));
			System.out.println("Socket is connected to port " + (port + 2000));

			selector = Selector.open();
			channel.configureBlocking(false);

			System.out.println("Selected opened");

			channel.register(selector, SelectionKey.OP_READ);
			System.out.println("Selected - 0");
		}

		public void stop() throws IOException {
			selector.close();
			channel.disconnect();
			channel.close();
			channel.socket().close();
		}

		public void run() {
			try {
				// selector.select();
				int count = channel.read(buffer);
				if (count != 160) {
					System.out.println("BAD!BAD!BAD!BAD! " + count);
				}

				buffer.flip();
				buffer.clear();
				if (count > 0) {
					ticks.add(System.currentTimeMillis());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void printTicks() {
			System.out.println("Packets " + ticks.size());
			long diff;
			long jitter = 0;
			for (int i = 1; i < ticks.size(); i++) {
				diff = (ticks.get(i) - ticks.get(i - 1));
				System.out.println("diff =" + diff);
				jitter = jitter + (diff - 20);
			}
			System.out.println("Jitter avg = " + jitter);
		}
	}

	static class ThreadFactoryImpl implements ThreadFactory {

		final ThreadGroup group;
		static final AtomicInteger msProviderPoolNumber = new AtomicInteger(1);
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;

		ThreadFactoryImpl() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "MsProviderImpl-FixedThreadPool-" + msProviderPoolNumber.getAndIncrement() + "-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != Thread.MIN_PRIORITY) {
				t.setPriority(Thread.MIN_PRIORITY);
			}
			return t;
		}
	}
}
