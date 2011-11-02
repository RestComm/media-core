package org.mobicents.media.server.impl.resource.ss7;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.util.FastMap;

import org.apache.log4j.Logger;
import org.mobicents.protocols.ss7.m3ua.M3UAChannel;
import org.mobicents.protocols.ss7.m3ua.M3UAProvider;
import org.mobicents.protocols.ss7.m3ua.M3UASelectionKey;
import org.mobicents.protocols.ss7.m3ua.M3UASelector;
import org.mobicents.protocols.ss7.m3ua.M3UAServerChannel;
import org.mobicents.protocols.ss7.m3ua.impl.tcp.TcpProvider;
import org.mobicents.protocols.ss7.m3ua.message.M3UAMessage;
import org.mobicents.protocols.ss7.m3ua.message.MessageClass;
import org.mobicents.protocols.ss7.m3ua.message.MessageType;
import org.mobicents.protocols.ss7.m3ua.message.TransferMessage;
import org.mobicents.protocols.ss7.m3ua.message.parm.ProtocolData;
import org.mobicents.protocols.stream.api.SelectorKey;
import org.mobicents.ss7.linkset.oam.Layer4;
import org.mobicents.ss7.linkset.oam.Linkset;
import org.mobicents.ss7.linkset.oam.LinksetManager;
import org.mobicents.ss7.linkset.oam.LinksetSelector;
import org.mobicents.ss7.linkset.oam.LinksetStream;

public class M3UALayer4 implements Layer4 {

	private static Logger logger = Logger.getLogger(M3UALayer4.class);

	byte[] rxBuffer;
	byte[] txBuffer;

	private ConcurrentLinkedQueue<byte[]> mtpqueue = new ConcurrentLinkedQueue<byte[]>();
	private ConcurrentLinkedQueue<byte[]> m3uaqueue = new ConcurrentLinkedQueue<byte[]>();

	private int OP_READ_WRITE = 3;

	private String address = null;
	private int port = 2727;

	private M3UAProvider m3uaProvider;
	private M3UAServerChannel serverChannel;
	private M3UAChannel channel;
	private M3UASelector selector;

	private LinksetSelector linkSetSelector = new LinksetSelector();
	private LinksetStream linksetStream = null;

	private LinksetManager linksetManager = null;

	private boolean started = false;

	public void add(Linkset linkset) {
		try {
			linksetStream = linkset.getLinksetStream();
			linksetStream.register(this.linkSetSelector);
		} catch (IOException ex) {
			logger.error(String.format(
					"Registration for %s LinksetStream failed", linkset
							.getName()), ex);
		}
	}

	public void remove(Linkset linkset) {
	    
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public LinksetManager getLinksetManager() {
		return linksetManager;
	}

	public void setLinksetManager(LinksetManager linksetManager) {
		this.linksetManager = linksetManager;
	}

	public void start() throws Exception {
		// M3UA
		m3uaProvider = TcpProvider.open();
		selector = m3uaProvider.openSelector();

		serverChannel = m3uaProvider.openServerChannel();
		serverChannel.bind(new InetSocketAddress(address, port));

		logger.info(String.format("M3UA Bound to %s:%d", address, port));
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		this.linksetManager.setLayer4(this);
		
		//Add all linkset stream
        FastMap<String, Linkset> map = this.linksetManager.getLinksets();
        for (FastMap.Entry<String, Linkset> e = map.head(), end = map.tail(); (e = e
                .getNext()) != end;) {
            Linkset value = e.getValue();
            this.add(value);
        }		

		this.started = true;

	}

	public void stop() throws Exception {
		this.started = false;
	}

	protected void perform() throws IOException {

		if (!started) {
			return;
		}
		Collection<SelectorKey> selected = linkSetSelector.selectNow(
				OP_READ_WRITE, 1);

		for (SelectorKey key : selected) {
			((LinksetStream) key.getStream()).read(rxBuffer);

			// Read data
			if (rxBuffer != null) {
				mtpqueue.offer(rxBuffer);
			}

			// write to stream
			if (!m3uaqueue.isEmpty()) {
				txBuffer = m3uaqueue.poll();
				if (txBuffer != null) {
					linksetStream.write(txBuffer);
				}
			}
		}

		// M3UA
		Collection<M3UASelectionKey> selection = selector.selectNow();
		for (M3UASelectionKey key : selection) {
			if (key.isAcceptable()) {
				accept((M3UAServerChannel) key.channel());
			}

			if (key.isReadable()) {
				if (logger.isTraceEnabled()) {
					logger
							.trace("Transmitting data from M3UA channel to hardware");
				}
				read((M3UAChannel) key.channel());
			}

			if (key.isWritable()) {
				if (logger.isTraceEnabled()) {
					logger
							.trace("Transmitting data from hardware to M3UA channel");
				}
				write((M3UAChannel) key.channel());
			}
		}

	}

	private void accept(M3UAServerChannel serverChannel) throws IOException {
		this.channel = serverChannel.accept();
		this.channel.register(selector, SelectionKey.OP_READ
				| SelectionKey.OP_WRITE);
		this.logger.info("Connected with " + channel);
	}

	private void read(M3UAChannel channel) throws IOException {
		M3UAMessage message = channel.receive();
		switch (message.getMessageClass()) {
		case MessageClass.TRANSFER_MESSAGES:
			switch (message.getMessageType()) {
			case MessageType.PAYLOAD:
				TransferMessage msg = (TransferMessage) message;
				m3uaqueue.add(msg.getData().getMsu());
			}
			break;
		default:
			logger.info("Unknown message class: " + message.getMessageClass());
		}
	}

	private void write(M3UAChannel channel) throws IOException {
		if (mtpqueue.isEmpty()) {
			return;
		}

		logger.info("Sending MSU to the remote peer");
		byte[] msu = mtpqueue.poll();

		if (msu != null) {
			TransferMessage msg = (TransferMessage) m3uaProvider
					.getMessageFactory()
					.createMessage(MessageClass.TRANSFER_MESSAGES,
							MessageType.PAYLOAD);
			ProtocolData data = m3uaProvider.getParameterFactory()
					.createProtocolData(0, msu);
			msg.setData(data);
			channel.send(msg);
			logger.info("Message sent");
		}
	}

}
