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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;

import org.mobicents.media.Format;
import org.mobicents.media.Server;
import org.mobicents.media.server.NtpTimeStampUtil;
import org.mobicents.media.server.impl.clock.LocalTask;
import org.mobicents.media.server.impl.clock.Scheduler;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtcp.RtcpReceptionReport;
import org.mobicents.media.server.impl.rtcp.RtcpReceptionReportItem;
import org.mobicents.media.server.impl.rtcp.RtcpSdes;
import org.mobicents.media.server.impl.rtcp.RtcpSdesChunk;
import org.mobicents.media.server.impl.rtcp.RtcpSdesItem;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.clock.Task;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.rtp.AVProfile;
import org.mobicents.media.server.spi.rtp.RtpSocket;

/**
 * @author amit bhayani
 * @author Oleg Kulikov
 */
public class RtpSocketImpl implements RtpSocket {

    private static int GEN = 1;
    private String id;    
    // After every 5000 ms RTCP report will be sent
    public static final int RTCP_MIN_TIME = 5000;    
    
    //If there is no RTP or RTCP received till RTCP_CUT_OFF_TIME, notify 
    //listener to take appropriate action
    public static final int RTCP_CUT_OFF_TIME = 2 * RTCP_MIN_TIME;
    
    /** Media type */
    protected MediaType mediaType; 
    
    //RTP local address and port pair
    private DatagramSocket rtpSocket;
    private DatagramChannel rtpChannel;    
    
    //RTCP local address and port pair
    private DatagramSocket rtcpSocket;
    private DatagramChannel rtcpChannel;
    private String localAddress;    
    
    //remote address and port pair
    protected InetSocketAddress rtpRemoteAddress = null;
    protected InetSocketAddress rtcpRemoteAddress = null;    
    
    //jitter in milliseconds. default value is 60ms
    private int jitter = 60;    
    
    //media streams
    private ReceiveStream receiveStream;
    private SendStream sendStream;    
    
    //factory instance
    private RtpFactory factory = null;
    
    //listener instance
    private RtpSocketListener listener;    
    
    //the default format map between rtp payload and format
    private AVProfile avProfile = new AVProfile();        
    //collection of codecs
    protected Collection<Codec> codecs;    
    
    //RTP clock instance
    private RtpClock clock;    
    
    //the format and rtp payload number negotiatiated for conversation
    //this values are valid after negotiation and till this socket will be released
    private Format format;
    private int payloadId;
    
    private SelectionKey rtpSelection;
    private SelectionKey rtcpSelection;
    
    protected boolean registered = false;
    private ByteBuffer sendBuffer = ByteBuffer.allocateDirect(8192);    
    
    //statistics
    private int rtpPacketsSent;
    private int rtpPacketsReceived;
    private int rtcpPacketsSent;
    private int rtcpPacketsReceived;
    private long rtcpOctetSent;
    private long rtcpOctetReceived;
    private long lastRtcpPackReceivedTimeStamp;
    private long lastRtcpSenderRprtReceivedTimeStamp;
    
    /**
     * Expected packets calculated on every RTCP transmission timeout and used as previous expected packets on next
     * transmission timeout for calculation of Fraction
     */
    private int intervalExpectedRtpPackets;
    
    /**
     * Actual received packets calculated on every transmission timeout and used as previous actual received packets on
     * next transmission timeout for calculation of Fraction
     */
    private int intervalReceivedRtpPackets;
    private RtcpSenderReport lastRtcpSenderReport = null;
    private volatile boolean isClosed = false;
    
    /**
     * The prefferedPort is assigned by RtpFactory. 
     * RtpSocket will reuse the prefferedPort to bind the Socket everytime
     * till BindException is thrown.
     * 
     *  Once BindException, a new Port is obtained from RtpFactory and it becomes 
     *  as prefferedPort for all future binds
     */
    private int rtpPreferredPort;
    private int rtcpPreferredPort;

    /**
     * The reference to last RTP Packet sent. Used for creation of RTCP SR
     */
    private RtpPacket lastRtpPackSent;
    
    private Scheduler scheduler = Server.scheduler;
    
    private byte[] rtcpPacketRawData = new byte[8192];
    
    private RtcpSendStream rtcpSendStream = null;
    private RtpPacketHandler rtpPacketHandler = null;
    private RtcpPacketHandler rtcpPacketHandler = null;
    
    private LocalTask rtcpSenderWorker;
    private String cname;

    private boolean silenceSuppression;
    
    /**
     * Creates a new instance of RtpSocket
     * 
     * @param timer
     *            used to synchronize receiver stream.
     * @param rtpMap
     *            RTP payloads list.
     */
    public RtpSocketImpl(RtpFactory factory, Collection<Codec> codecs, MediaType media, boolean silenceSuppression) throws IOException, ResourceUnavailableException {
        this.id = this.genID();
        this.mediaType = media;
        this.clock = factory.getClock(media);
        this.factory = factory;
        
        //assigns default preconfgured value for silence suppression
        this.silenceSuppression = silenceSuppression;
        
        
        this.codecs = codecs;
        this.avProfile = factory.getAVProfile().clone();

        jitter = factory.getJitter();

        sendStream = new SendStream(this, this.avProfile);
        receiveStream = new ReceiveStream(this, factory.getJitter(), this.avProfile);

        this.localAddress = factory.getBindAddress();
        this.cname = "MMS-" + id + "-" + mediaType.getName() + "@" + localAddress;

        this.rtcpSendStream = new RtcpSendStream(this);

        this.rtpPacketHandler = new RtpPacketHandler(this);
        this.rtcpPacketHandler = new RtcpPacketHandler(this);
    }

    
    /**
     * Generates unique identifier for this connection.
     * 
     * @return hex view of the unique integer.
     */
    private String genID() {
        GEN++;
        if (GEN == Integer.MAX_VALUE) {
            GEN = 1;
        }
        return Integer.toHexString(GEN);
    }

    /**
     * Opens channel.
     * 
     * @throws java.io.IOException
     */
    private void openRtpChannel() throws IOException {
        rtpChannel = DatagramChannel.open();
        rtpChannel.configureBlocking(false);

        rtpSocket = rtpChannel.socket();
    }

    private void openRtcpChannel() throws IOException {
        rtcpChannel = DatagramChannel.open();
        rtcpChannel.configureBlocking(false);

        rtcpSocket = rtcpChannel.socket();
    }

    private void closeRtpSocket() {
        try {
            if (rtpChannel != null) {
                rtpChannel.close();
            }

            if (rtpSocket != null) {
                rtpSocket.disconnect();
                rtpSocket.close();
            }
        } catch (IOException e) {
        }
    }

    private void closeRtcpSocket() {
        try {
            if (rtcpChannel != null) {
                rtcpChannel.close();
            }

            if (rtcpSocket != null) {
                rtcpSocket.disconnect();
                rtcpSocket.close();
            }
        } catch (IOException e) {
        }
    }

    /**
     * Binds Datagram to the address sprecified.
     * 
     * @throws java.io.IOException
     * @throws org.mobicents.media.server.spi.ResourceUnavailableException
     */
    public void bind() throws IOException, ResourceUnavailableException {
        //disbale closed flag if was set
        this.isClosed = false;
        boolean portCycled = false;
        this.rtpPreferredPort = factory.getNextEvenPort();
        this.rtcpPreferredPort = factory.getNextOddPort();

        int initialPort = this.rtpPreferredPort;

        //opening RTP channel
        openRtpChannel();

        //binding socket to the first available port.
        while (!rtpSocket.isBound()) {
            try {
                //it is expected that "preffered port" is what we need
                InetSocketAddress address = new InetSocketAddress(factory.getBindAddress(), this.rtpPreferredPort);
                rtpSocket.bind(address);
            } catch (SocketException e) {
                //port is in use? let's try another
                this.rtpPreferredPort = this.factory.getNextEvenPort();

                if (this.rtpPreferredPort <= initialPort) {
                    portCycled = true;
                }

                if (portCycled && this.rtpPreferredPort > initialPort) {
                    //At this point one full cycle of ports are scanned and there are no free ports. Better to exit
                    break;
                }
                continue;
            }
        }//while loop

        //RTP socket still not bound? throw exception
        if (!rtpSocket.isBound()) {
            rtpChannel.close();
            throw new ResourceUnavailableException();
        }

        initialPort = this.rtcpPreferredPort;
        portCycled = false;

        //Opening RTCP channel
        openRtcpChannel();

        while (!rtcpSocket.isBound()) {
            try {
                InetSocketAddress address = new InetSocketAddress(localAddress, this.rtcpPreferredPort);
                rtcpSocket.bind(address);
            } catch (SocketException e) {
                //Port is not free. let's try another
                this.rtcpPreferredPort = this.factory.getNextOddPort();


                if (this.rtcpPreferredPort <= initialPort) {
                    portCycled = true;
                }

                if (portCycled && this.rtcpPreferredPort > initialPort) {
                    //At this point one full cycle of ports are scanned and there are no free ports. Better to exit
                    break;
                }
            }
        }

        //RTCP socket still not bound? throw exception
        if (!rtcpSocket.isBound()) {
            rtcpChannel.close();
            this.closeRtpSocket();
            throw new ResourceUnavailableException();
        }
    }

    /**
     * Registers this socket usign specified selector.
     * 
     * @param selector the selector for registration.
     * @throws java.nio.channels.ClosedChannelException
     */
    public void register(Selector selector) throws ClosedChannelException {
        rtpSelection = rtpChannel.register(selector, SelectionKey.OP_READ, this.rtpPacketHandler);
        rtcpSelection = rtcpChannel.register(selector, SelectionKey.OP_READ, this.rtcpPacketHandler);
        registered = true;
    }

    /**
     * Gets the currently used format.
     * 
     * @return the format instance.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Gets the list of used codecs.
     * 
     * @return list of codecs.
     */
    public Collection<Codec> getCodecs() {
        return codecs;
    }

    /**
     * Specifies format and payload id which will be used by this socket for transmission 
     * 
     * This methods should be used by other components which are responsible for SDP negotiation.
     * The socket itself can not negotiate SDP. 
     * 
     * 
     * @param payloadId rtp payload number
     * @param format the format object.
     */
    public void setFormat(int payloadId, Format format) {
        //checking input parameters
        if (payloadId < 0) {
            throw new IllegalArgumentException("Illegal payload number");
        }

        if (format == null) {
            throw new IllegalArgumentException("Format can not be null");
        }

        // No any checks with formatConfig!
        // format conf is used as default configuration for creating local session 
        // description when remote session description is not known yet.
        // just apply values as is
        this.payloadId = payloadId;
        this.format = format;

        //initialize streams
        sendStream.setFormat(payloadId, format);
        receiveStream.setFormat(payloadId, format);
    }

    /**
     * Assigns RFC2833 DTMF playload number.
     * 
     * @param dtmf the DTMF payload number.
     */
    public void setDtmfPayload(int dtmf) {
        receiveStream.setDtmf(dtmf);
        sendStream.setDtmf(dtmf);
    }

    /**
     * Modifies silence suppression flag.
     * 
     * @param value the new value for flag.
     */
    public void setSilenceSuppression(boolean value) {
        this.silenceSuppression = value;
    }
    
    /**
     * Indicates the status of silence suppression algorithm.
     * 
     * @return true if silence suppression is enabled.
     */
    public boolean isSilenceSuppressed() {
        return this.silenceSuppression;
    }
    /**
     * Gets address to which this socked is bound.
     * 
     * @return either local address to which this socket is bound or public
     *         address in case of NAT translation.
     */
    public String getLocalAddress() {
        return localAddress;
    }

    /**
     * Returns port number to which this socked is bound.
     * 
     * @return port number or -1 if socket not bound.
     */
    public int getLocalPort() {
        return this.rtpPreferredPort;
    }

    /**
     * Gets the jitter for time of packet arrival
     * 
     * @return the value of jitter in milliseconds.
     */
    public int getJitter() {
        return this.jitter;
    }

    /**
     * Assign new value of packet time arrival jitter.
     * 
     * @param jitter
     *            the value of jitter in milliseconds.
     */
    public void setJitter(int jitter) {
        this.jitter = jitter;
    }

    /**
     * Assign RTP clock implementation.
     * 
     * @param clock the RTP clock instance;
     */
    public void setClock(RtpClock clock) {
        this.clock = clock;
    }

    /**
     * Gets current RTP clock instance.
     * 
     * @return the RTP clock instance.
     */
    public RtpClock getClock() {
        return clock;
    }

    /**
     * Gets receiver stream.
     * 
     * @return receiver stream instance.
     */
    public ReceiveStream getReceiveStream() {
        return receiveStream;
    }

    /**
     * Gets currently used audio/video profile
     * @return
     */
    public AVProfile getAVProfile() {
        return this.avProfile;
    }

    /**
     * Gets the number of received packets
     * 
     * @return the number of packets received
     */
    public int getPacketsReceived() {
        return rtpPacketsReceived;
    }

    /**
     * Gets the number of sent packets
     * 
     * @return the number of sent packets.
     */
    public int getPacketsSent() {
        return rtpPacketsSent;
    }

    /**
     * Closes this socket and resets its streams;
     * This method is called by RtpSocket user.
     * 
     */
    public void release() {
        //channel close action should be synchronized with read and write 
        //but we want to avid usage of any locks so at this stage we are 
        //setting flag that socket is closed only!
        //
        //Receiver will check this flag on selected channel and perform actual close procedure.
        this.isClosed = true;
        if (rtcpSenderWorker != null) {
            this.rtcpSenderWorker.cancel();
        }
    }

    public void close() {
        if (rtpSelection != null) {
            rtpSelection.cancel();
        }
        //reset streams
        receiveStream.reset();
        sendStream.reset();

        //disable format and payload
        this.format = null;
        this.payloadId = -1;

        //clean stats
        rtpPacketsSent = 0;
        rtpPacketsReceived = 0;

        this.closeRtpSocket();

        if (rtcpSelection != null) {
            rtcpSelection.cancel();
        }

        this.lastRtcpPackReceivedTimeStamp = 0;
        this.rtcpOctetReceived = 0;
        this.rtcpPacketsReceived = 0;

        this.rtcpOctetSent = 0;
        this.rtcpOctetReceived = 0;

        this.intervalExpectedRtpPackets = 0;
        this.intervalReceivedRtpPackets = 0;

        this.lastRtpPackSent = null;

        this.rtcpSendStream.reset();
        this.closeRtcpSocket();
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    /**
     * Gets the currently assigned listener.
     * 
     * @return the listener instance.
     */
    public RtpSocketListener getListener() {
        return listener;
    }

    /**
     * Assigns listener which will receive notifications.
     * 
     * @param listener the listener instance.
     */
    public void setListener(RtpSocketListener listener) {
        this.listener = listener;
    }

    /**
     * Assigns remote end.
     * 
     * @param address
     *            the address of the remote party.
     * @param port
     *            the port number of the remote party.
     */
    public void setPeer(InetAddress address, int port) throws IOException {
        rtpRemoteAddress = new InetSocketAddress(address, port);
        rtpChannel.connect(rtpRemoteAddress);

        rtcpRemoteAddress = new InetSocketAddress(address, port + 1);
        rtcpChannel.connect(rtcpRemoteAddress);

        factory.registerQueue.offer(this);

        // Add RtcpSendStream to scheduler to send RTCP Report every 5 secs
        this.rtcpSenderWorker = scheduler.execute(rtcpSendStream);

    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.impl.rtp.RtpSocket#startSendStream(PushBufferDataSource);
     */
    public SendStream getSendStream() {
        return sendStream;
    }

    private void send(RtcpPacket rtcpPacket) {
        // TODO optimize the RTCPPacket to use the ByteBuffer directly
        int length = rtcpPacket.encode(rtcpPacketRawData, 0);

        this.rtcpPacketsSent += rtcpPacket.getNoOfPackets();
        this.rtcpOctetSent += length;
        // converting packet to binary array and sent to the remote address.
        sendBuffer.clear();
        sendBuffer.rewind();
        sendBuffer.put(rtcpPacketRawData, 0, length);
        sendBuffer.flip();
        try {
            rtcpChannel.write(sendBuffer);
        } catch (IOException e) {
            this.notify(e);
        }
    }

    /**
     * Sends media data to remote peer.
     * 
     * This method uses blocking sending to make sure that data is out in time.
     * 
     * @param RtpPacket - 
     *            the packet which contains media data and rtp header
     * @throws java.io.IOException
     */
    public void send(RtpPacket packet) throws IOException {
        //check state flag before sending
        if (!this.isClosed) {

            //Reference to last RTP Packet Sent and time when it was sent
            //this.lastRtpPackSentTimeStamp = scheduler.getTimestamp();
            this.lastRtpPackSent = packet;

            byte[] p = packet.toByteArray();
            send(p);
        }
    }

    /**
     * Sends rtp packet to the remote peer.
     * 
     * @param packet the rtp packet as binary arrary
     * @throws java.io.IOException
     */
    public void send(byte[] packet) throws IOException {
        //coverting packet to binary array and sent to the remote address.
        sendBuffer.clear();
        sendBuffer.rewind();
        sendBuffer.put(packet);
        sendBuffer.flip();
        try {
            rtpChannel.write(sendBuffer);
        } catch (PortUnreachableException e) {
        } catch (IOException e) {
            this.notify(e);
        }
        rtpPacketsSent++;
    }

    /**
     * This method is called when rtp socket receives new rtp frame.
     * 
     * @param rtpPacket
     */
    public void receiveRtp(RtpPacket rtpPacket) {
        receiveStream.process(rtpPacket);
        rtpPacketsReceived++;
    }

    public void receiveRtcp(RtcpPacket rtcpPacket) {

        RtcpReceptionReportItem[] rtcpReceptionReports = null;

        // Keep reference to timeStamp when last RTCP was received
        this.lastRtcpPackReceivedTimeStamp = scheduler.getTimestamp();

        this.rtcpOctetReceived += rtcpPacket.getPacketSize();
        this.rtcpPacketsReceived += rtcpPacket.getNoOfPackets();

        if (rtcpPacket.getRtcpSenderReport() != null) {
            this.lastRtcpSenderReport = rtcpPacket.getRtcpSenderReport();
            this.lastRtcpSenderRprtReceivedTimeStamp = this.lastRtcpPackReceivedTimeStamp;

            rtcpReceptionReports = this.lastRtcpSenderReport.getRtcpReceptionReports();
        }

    //if(rtcpReceptionReports != null){
    //TODO : sender may modify its transmissions based on the feedback. Define events to be fired for Listener
    //}

    }

    /**
     * Notifies the listener that something goes wrong.
     * 
     * @param e the exception. 
     */
    public void notify(Exception e) {
        if (listener != null) {
            listener.error(e);
        }
    }

    public boolean rtcpExpired() {
        long now = scheduler.getTimestamp();
        if (now - this.lastRtcpPackReceivedTimeStamp > RTCP_CUT_OFF_TIME) {
            return true;
        }
        return false;
    }
    /**
     * Statistical method.
     * 
     * @return the number of bytes received.
     */
    public long getBytesReceived() {
        return receiveStream.byteCount;
    }

    /**
     * Statistical method.
     * 
     * @return the number of bytes sent.
     */
    public long getBytesSent() {
        return sendStream.byteCount;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    class RtcpSendStream implements Task {

        private RtpSocketImpl rtpSocketImpl = null;
        private boolean initial = true;

        RtcpSendStream(RtpSocketImpl rtpSocketImpl) {
            this.rtpSocketImpl = rtpSocketImpl;
        }

        void reset() {
            this.initial = true;
        }

        public void cancel() {
            // TODO Auto-generated method stub
        }

        public boolean isActive() {
            return !isClosed;
        }

        public int perform() {
            // If for first time we will send the report after 2.5 seconds. After this every 5 seconds
            if (initial) {
                this.initial = false;
                return (RTCP_MIN_TIME / 2);
            }


            RtcpSenderReport rtcpSenderReport = null;
            RtcpReceptionReport rtcpReceptionReport = null;
            RtcpSdes rtcpSdes = null;
            // RtcpAppDefined rtcpAppDefined = null;

            long now = Server.scheduler.getTimestamp();
            // rtpSocketImpl.setLastRtcpPackSentTimeStamp(now);

            JitterBuffer jitBuff = receiveStream.getJitterBuffer();

            int expectedPackets = jitBuff.getExpectedPacketCount();
            int receivedPackets = rtpSocketImpl.getPacketsReceived();

            RtpPacket rtpPacket = lastRtpPackSent;

            if (rtpPacket != null) {
                long[] ntpTs = NtpTimeStampUtil.toNtpTime(now);

                rtcpSenderReport = new RtcpSenderReport(false, rtpPacket.getSyncSource(), ntpTs[0], ntpTs[1], rtpPacket.getTimestamp(), rtpSocketImpl.getPacketsSent(), rtpSocketImpl.getBytesSent());

                rtcpSdes = new RtcpSdes(false);

                RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, cname);
                RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk(rtpPacket.getSyncSource());
                rtcpSdesChunk.addRtcpSdesItem(rtcpSdesItem);

                rtcpSdes.addRtcpSdesChunk(rtcpSdesChunk);
            }

            // TODO If no RTP sent for time > 2T raise RTCPSendTimeout event. May be indicator that silence started? But
            // do we need it? If needed and implemented we call rtcpSenderTimeout on listener
            //if (listener != null) {
            //    listener.rtcpSenderTimeout(this.rtpSocketImpl);
            //}


            rtpPacket = jitBuff.getLastRtpPacketRecd();

            if (rtpPacket != null) {
                int lost = expectedPackets - receivedPackets;

                int expected_interval = (expectedPackets - intervalExpectedRtpPackets);
                int received_interval = (receivedPackets - intervalReceivedRtpPackets);

                int lost_interval = expected_interval - received_interval;
                int fraction = 0;
                if (expected_interval == 0 || lost_interval <= 0) {
                    fraction = 0;
                } else {
                    fraction = (lost_interval << 8) / expected_interval;
                }

                // The middle 32 bits out of 64 in the NTP timestamp (as explained in
                // Section 4) received as part of the most recent RTCP sender report
                // (SR) packet from source SSRC_n. If no SR has been received yet,
                // the field is set to zero.
                long lsr = 0;

                // The delay, expressed in units of 1/65536 seconds, between
                // receiving the last SR packet from source SSRC_n and sending this
                // reception report block.
                long dsr = 0;

                if (lastRtcpSenderReport != null) {
                    lsr |= lastRtcpSenderReport.getNtpSec() & 0x0000FFFF;
                    lsr <<= 16;
                    lsr |= ((lastRtcpSenderReport.getNtpFrac() & 0xFFFF0000) >> 16);

                    dsr = ((now - lastRtcpSenderRprtReceivedTimeStamp) * 65536) / 1000;
                }

                RtcpReceptionReportItem rtcpReceptionReportItem = new RtcpReceptionReportItem(
                        rtpPacket.getSyncSource(), fraction, lost, jitBuff.getSeqNoCycles(), rtpPacket.getSeqNumber(),
                        (int) receiveStream.getInterArrivalJitter(), lsr, dsr);

                if (rtcpSenderReport == null) {
                    rtcpReceptionReport = new RtcpReceptionReport(false,
                            receiveStream.getSsrc());
                    rtcpReceptionReport.addRtcpReceptionReportItem(rtcpReceptionReportItem);

                    rtcpSdes = new RtcpSdes(false);

                    RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(
                            RtcpSdesItem.RTCP_SDES_CNAME, cname);
                    RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk(receiveStream.getSsrc());
                    rtcpSdesChunk.addRtcpSdesItem(rtcpSdesItem);

                    rtcpSdes.addRtcpSdesChunk(rtcpSdesChunk);
                } else {
                    rtcpSenderReport.addRtcpReceptionReportItem(rtcpReceptionReportItem);
                }
            }

            // If no RTP and RTCP received for 2T fire RTCPReceiveTimeout event.
            // 1. This is useful when Connection is RECV_ONLY and far end is crashed.
            // 2. If Connection is SEND_RECV and far end crashed, the RTPSocket will anyway throw error and this
            // Connection will be deleted
            // 3. If Connection is SEND_RECV and silence is ON on MMS side and far end crashed, even then this algo will
            // work. But if far end doesn't
            // have RTCP implemented, this algo will wrongly indicate ReceiverTimeout. This is punishment for not
            // implementing RTCP ;)
            if ((now - jitBuff.getLastRtpPackReceivedTimeStamp()) > RtpSocketImpl.RTCP_CUT_OFF_TIME && (now - lastRtcpPackReceivedTimeStamp) > RtpSocketImpl.RTCP_CUT_OFF_TIME) {
                if (listener != null) {
                    listener.rtcpReceiverTimeout(this.rtpSocketImpl);
                }
            }

            // set the reference to expected and received packets here, so they can be referenced on next transmission
            // timeout for calculation of Fraction
            intervalExpectedRtpPackets = expectedPackets;
            intervalReceivedRtpPackets = receivedPackets;

            RtcpPacket rtcpPacket = new RtcpPacket(rtcpSenderReport,
                    rtcpReceptionReport, rtcpSdes, null, null);

            send(rtcpPacket);


            return RTCP_MIN_TIME;
        }
    }
}
