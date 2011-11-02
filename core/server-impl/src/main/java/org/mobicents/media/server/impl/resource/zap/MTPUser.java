/*
 * Mobicents, Communications Middleware
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors. All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA 02110-1301 USA
 */
package org.mobicents.media.server.impl.resource.zap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.mobicents.media.Server;
import org.mobicents.media.server.impl.clock.LocalTask;
import org.mobicents.media.server.spi.clock.Task;
import org.mobicents.protocols.link.LinkState;
import org.mobicents.protocols.link.LinkStateListener;
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
import org.mobicents.protocols.ss7.mtp.Mtp1;
import org.mobicents.protocols.ss7.mtp.Mtp2;
import org.mobicents.protocols.ss7.mtp.Mtp3;
import org.mobicents.protocols.ss7.mtp.Mtp3Listener;

/**
 *
 * @author kulikov
 */
public class MTPUser implements Task, Mtp3Listener, LinkStateListener {

    private String name;
    /** Originated point code */
    private int opc;
    /** Destination point code */
    private int dpc;
    /** Sub-Service */
    private int ni;
    /** physical channels */
    private List<Mtp1> channels;
    /** MTP layer 3 */
    private Mtp3 mtp3;
    private Logger logger = Logger.getLogger(MTPUser.class);
    private volatile boolean isActive = false;
    private LocalTask task;

    private InetSocketAddress address;
    
    private String localAddress;
    private int localPort;
        
    private ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue();
    
    private M3UAProvider m3uaProvider;
    private M3UAServerChannel serverChannel;
    private M3UAChannel channel;
    private M3UASelector selector;
    
    /**
     * Gets the name of the linkset.
     *
     * @return the name of the linkset
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the linkset.
     *
     * @param name the alhanumeric name of the linkset.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Assigns originated point code
     *
     * @param opc the value of the originated point code in decimal format
     */
    public void setOpc(int opc) {
        this.opc = opc;
    }

    /**
     * Assigns destination point code.
     *
     * @param dpc the destination point code value in decimal format.
     */
    public void setDpc(int dpc) {
        this.dpc = dpc;
    }

    /**
     * @return the opc
     */
    public int getOpc() {
        return opc;
    }

    /**
     * @return the dpc
     */
    public int getDpc() {
        return dpc;
    }
    /** 
     * Fetches Sub-Service part of SI octet.
     * @return
     */
    public int getNetworkIndicator() {
		return ni;
	}
    /**
     * Sets NI(Network Indicator) which should be used as part of SIO.
     * @param ssi
     */
	public void setNetworkIndicator(int ni) {
		this.ni = ni & 0x03;
	}

	public void setLocalAddress(String address) {
        this.localAddress = address;
    }
    
    public String getLocalAddress() {
        return localAddress;
    }
    
    public void setLocalPort(int port) {
        this.localPort = port;
    }
    
    public int getLocalPort() {
        return localPort;
    }
    
    /**
     * Assigns signalling channels.
     *
     * @param channels the list of available physical channels.
     */
    public void setChannels(List<Mtp1> channels) {
        this.channels = channels;
    }

    /**
     * Activates link set.
     */
    public void start() throws IOException, ClassNotFoundException {
        try {
            Server.scheduler.start();
            address = new InetSocketAddress(localAddress, localPort);
            
            m3uaProvider = TcpProvider.open();
            selector = m3uaProvider.openSelector();
            
            serverChannel = m3uaProvider.openServerChannel();
            serverChannel.bind(address);
            
            logger.info("Bound to " + address);            
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            logger.info("Creating MTP layer 3");
            
            ArrayList<Mtp2> linkset = new ArrayList();
            for (Mtp1 chann : channels) {
//                Mtp2 link = new Mtp2(name + "-" + chann.getCode(), chann);
//                linkset.add(link);
            }
            // assigning physical channel
            mtp3 = new Mtp3(name);
            mtp3.setDpc(this.dpc);
            mtp3.setOpc(this.opc);
            mtp3.setNetworkIndicator(this.ni);
            mtp3.setLinks(linkset);
            mtp3.addMtp3Listener(this);
            logger.info("Point codes are configured");

            // starting layer 3
            mtp3.start();
            this.isActive = true;
            task = Server.scheduler.execute(this);
        } catch (RuntimeException re) {
            re.printStackTrace();
            throw re;
        }
    }

    /**
     * Deactivates linkset.
     */
    public void stop() {
        task.cancel();

        //and close all channels
//        mtp3.close();
    }

    public void cancel() {
        this.isActive = false;
    }

    public boolean isActive() {
        return this.isActive;
    }

    private void accept(M3UAServerChannel serverChannel) throws IOException {
        this.channel = serverChannel.accept();
        this.channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        this.logger.info("Connected with " + channel);
    }
    
    private void read(M3UAChannel channel) throws IOException {
        M3UAMessage message = channel.receive();
        switch (message.getMessageClass()) {
            case MessageClass.TRANSFER_MESSAGES :
                switch (message.getMessageType()) {
                    case MessageType.PAYLOAD :
                       TransferMessage msg = (TransferMessage) message;
                       mtp3.send(msg.getData().getMsu(), msg.getData().getMsu().length);
                }
                break;
            default :
                logger.info("Unknown message class: " + message.getMessageClass());
        }
    }
    
    private void write(M3UAChannel channel) throws IOException {
        if (queue.isEmpty()) {
            return;
        }
        
        logger.info("Sending MSU to the remote peer");
        byte[] msu = queue.poll();
        TransferMessage msg = (TransferMessage) m3uaProvider.getMessageFactory().createMessage(
                    MessageClass.TRANSFER_MESSAGES, 
                    MessageType.PAYLOAD);
        ProtocolData data = m3uaProvider.getParameterFactory().createProtocolData(0, msu);
        msg.setData(data);
        channel.send(msg);        
        logger.info("Message sent");
    }
    
    public int perform() {
        //Poll MTP3
        mtp3.run();
       
        try {
            Collection<M3UASelectionKey> selection = selector.selectNow();
            for (M3UASelectionKey key : selection) {
                if (key.isAcceptable()) {
                    accept((M3UAServerChannel)key.channel());
                }
                
                if (key.isReadable()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Transmitting data from M3UA channel to hardware");
                    }
                    read((M3UAChannel) key.channel());
                }
                
                if (key.isWritable()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Transmitting data from hardware to M3UA channel");
                    }
                    write((M3UAChannel) key.channel());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return 1;
    }


    public void linkUp() {
        queue.clear();
    }

    public void linkDown() {
    }

    public void receive(byte[] data) {
        logger.info("Receive new MSU, queueing");
        queue.offer(data);
    }

    public void onStateChange(LinkState state) {
        if (state == LinkState.ACTIVE) {
            queue.clear();
        }
    }
}
