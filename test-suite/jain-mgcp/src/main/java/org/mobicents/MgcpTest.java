/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.JainMgcpStack;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

/**
 * 
 * @author kulikov
 */
public class MgcpTest {

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		new Thread(server).start();
		System.out.println("Starting server");

		Client client = new Client();
		System.out.println("Sending requests");

		for (int i = 0; i < 1; i++) {
			client.sendRequest();
		}

		Thread.sleep(5000);
		server.stopped = true;
		server.close();

		System.out.println("Passed = " + client.isPassed());
		client.shutdown();
	}
}

class Server implements Runnable {

	private DatagramSocket socket;
	public volatile boolean stopped = false;

	public Server() throws Exception {
		socket = new DatagramSocket(2727,InetAddress.getLocalHost());
	}

	@Override
	public void run() {
		byte[] buffer = new byte[8192];
		while (!stopped) {

			DatagramPacket p = new DatagramPacket(buffer, 8192);
			String msg = null;
			try {
				try {
					socket.receive(p);
				} catch (Exception e) {
					e.printStackTrace();
				}

				msg = new String(buffer, 0, p.getLength());
				String[] tokens = msg.split("\n");

				String header = tokens[0];
				tokens = header.split(" ");
				
				String resp = String.format("200 %s OK\n", tokens[1].trim());
				byte[] data = resp.getBytes();

				DatagramPacket response = new DatagramPacket(data, data.length, p.getAddress(), p.getPort());
				try {
					System.err.println("RCV: "+msg);
					socket.send(response);
				} catch (Exception e) {
				}
			} catch (Exception e) {
				System.err.println("MSG: '" + msg + "' \nbuffer: "+Arrays.toString(buffer));
				e.printStackTrace();

			}

		}
	}

	public void close() {
		socket.close();
	}
}

class Client implements JainMgcpListener {

	private JainMgcpStack mgcpStack;
	private JainMgcpProvider mgcpProvider;

	private volatile int txID = 1;
	private CallIdentifier callId = new CallIdentifier("FF");
	private EndpointIdentifier endpointID = new EndpointIdentifier("any", InetAddress.getLocalHost().getHostAddress()+":2727");

	private ConcurrentHashMap requests = new ConcurrentHashMap();

	public Client() throws CreateProviderException, UnknownHostException, TooManyListenersException {
		mgcpStack = new JainMgcpStackImpl(InetAddress.getLocalHost(), 2427);
		mgcpProvider = mgcpStack.createProvider();
		mgcpProvider.addJainMgcpListener(this);
	}

	public void sendRequest() {
		CreateConnection req = new CreateConnection(this, callId, endpointID, ConnectionMode.SendRecv);
		req.setTransactionHandle(txID);

		requests.put(txID, req);
		txID++;

		mgcpProvider.sendMgcpEvents(new JainMgcpEvent[] { req });
	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jmce) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent response) {
		int tx = response.getTransactionHandle();
		requests.remove(tx);
	}

	public boolean isPassed() {
		System.err.println("--->"+requests);
		return requests.isEmpty();
	}

	public void shutdown() throws DeleteProviderException {
		mgcpProvider.getJainMgcpStack().deleteProvider(mgcpProvider);
	}
}