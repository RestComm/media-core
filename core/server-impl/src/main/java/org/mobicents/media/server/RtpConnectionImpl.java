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
package org.mobicents.media.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

import org.mobicents.media.Format;
import org.mobicents.media.server.impl.rtp.ReceiveStream;
import org.mobicents.media.server.impl.rtp.RtpSocketImpl;
import org.mobicents.media.server.impl.rtp.RtpSocketListener;
import org.mobicents.media.server.impl.rtp.sdp.MediaDescriptor;
import org.mobicents.media.server.impl.rtp.sdp.SessionDescriptor;
import org.mobicents.media.server.resource.Channel;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.rtp.AVProfile;
import org.mobicents.media.server.spi.rtp.RtpManager;
import org.mobicents.media.server.spi.rtp.RtpSocket;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class RtpConnectionImpl extends ConnectionImpl implements RtpSocketListener {

    //session descriptors
    private String localDescriptor;
    private String remoteDescriptor;
    
    //media profile
    private int[][] payloads = new int[2][15];
    //keeps formats for each media type: formats[MediaType.getCode()][index]
    private Format[][] formats = new Format[2][15];
    
    //number of media types
    private int channelCount;
    //numbers of format per channel
    private int[] formatCount = new int[2];
    
    private RtpSocket[] sockets = new RtpSocket[2];
    private String bindAddress;
    
    private int prefferedPayload;
    private Format prefferedFormat;
    
    public RtpConnectionImpl(Endpoint endpoint, ConnectionFactory connectionFactory, RtpManager rtpFactory) throws ResourceUnavailableException {
        super(endpoint, connectionFactory);
        
        //Get media available media types
        Collection<MediaType> mediaTypes = endpoint.getMediaTypes();
        
        //Get rtp factory
        RtpManager factory = rtpFactory;
        bindAddress = factory.getBindAddress();
        
        for (MediaType mediaType : mediaTypes) {
            int i = mediaType.getCode();
            try {
                sockets[i] = factory.getRTPSocket(mediaType);
                ((RtpSocketImpl)sockets[i]).setListener(this);
            } catch (IOException e) {
                throw new ResourceUnavailableException(e);
            }
        }
    }
    
    @Override
    protected void join(MediaType mediaType){
    	super.join(mediaType);
        int i = mediaType.getCode();
        
        //RX formats with possible encodings
        Format[] rxFormats = new Format[15];
        int rxCount = 0;
        if (rxChannels[i] != null) {
            rxCount = getExtendedRxFormats(rxChannels[i].getInputFormats(), sockets[i].getCodecs(), rxFormats);
        }

        //TX formats with possible encodings
        Format[] txFormats = new Format[15];
        int txCount = 0;
        if (txChannels[i] != null) {
            txCount = getExtendedTxFormats(txChannels[i].getOutputFormats(), sockets[i].getCodecs(), txFormats);
        }
        
        HashMap<Integer, Format> profile = getProfile(sockets[i].getAVProfile(), mediaType);
        formatCount[i] = intersection(rxFormats, rxCount, txFormats, txCount, profile, 
                payloads[i], formats[i]);
    }
    
    @Override
    protected void join() {       
        Collection<MediaType> mediaTypes = ((BaseEndpointImpl)getEndpoint()).getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
            this.join(mediaType);
        }
    }
    
    
    @Override
    protected void bind(MediaType mediaType) throws ResourceUnavailableException {
    	if((this.stream & mediaType.getMask()) != 0x0){
            try {
                 sockets[mediaType.getCode()].bind();
             } catch (IOException e) {
                 throw new ResourceUnavailableException(e);
             }
             super.bind(mediaType);
    	} 
        //if connection is just bound then we need to update local descriptor    	
    	localDescriptor = null;
    }
    
    //This is just helper method if Controller doesn't care of MediaType.
    protected void bind() throws ResourceUnavailableException {
        Collection<MediaType> mediaTypes = ((BaseEndpointImpl)getEndpoint()).getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
           this.bind(mediaType);
        }
    }
    
    private HashMap<Integer, Format> getProfile(AVProfile avProfile, MediaType mediaType) {
        HashMap<Integer, Format> profile = new HashMap();
        switch (mediaType.getCode()) {
            case 0 :
                profile.putAll(avProfile.getAudioFormats());
                break;
            case 1 :
                profile.putAll(avProfile.getVideoFormats());
                break;
        }
        return profile;
    }
    
    private boolean formatPresent(Format[] srcFormat, Format formatToCheck, int count){
    	for(int i = 0; i < count; i++){
    		Format f = srcFormat[i];
    		if(f.matches(formatToCheck)){
    			return true;
    		}
    	}
    	return false;
    }
    
    private int getExtendedRxFormats(Format[] formats, Collection<Codec> codecs, Format[] extended) {
        int count = 0;
        for (Format fmt : formats) {
        	
        	if(formatPresent(extended, fmt, count)){
        		continue;
        	}
        	
            extended[count++] = fmt;
            if (fmt == Format.ANY) {
                break;
            }
            for (Codec c : codecs) {
                if (c.getSupportedOutputFormat().matches(fmt)) {
                    boolean present = formatPresent(extended, c.getSupportedInputFormat(), count);
                    if (!present) {
                        extended[count++] = c.getSupportedInputFormat();
                    }
                }
            }
        }
        return count;
    }

    private int getExtendedTxFormats(Format[] formats, Collection<Codec> codecs, Format[] extended) {
        int count = 0;
        for (Format fmt : formats) {
        	
        	if(formatPresent(extended, fmt, count)){
        		continue;
        	}
        	
            extended[count++] = fmt;
            if (fmt == Format.ANY) {
                break;
            }
            for (Codec c : codecs) {
                if (c.getSupportedInputFormat().matches(fmt)) {
                    boolean present = formatPresent(extended, c.getSupportedOutputFormat(), count);
                    if (!present) {
                        extended[count++] = c.getSupportedOutputFormat();
                    }
                }
            }
        }
        return count;
    }
    
    private int intersection(Format[] rxFormats, int rxCount, 
            Format[] txFormats, int txCount, 
            HashMap<Integer, Format> profile, 
            int[] payloads,
            Format[] formats) {
        Collection<Integer> keys = profile.keySet();
        int count = 0;
        for (Integer key: keys) {
            //get format from profile
            Format f = profile.get(key);
        
            //if RFC2833 event is presented in profile then
            //add it because it will be transformed into actual media format;
            if (f.matches(AVProfile.DTMF)) {
                payloads[count] = key;
                formats[count++] = f;
                continue;
            }
            
            //checking format in rx array
            boolean found = rxCount == 0;
            for (int i = 0; i < rxCount; i++) {
                if (f.matches(rxFormats[i])) {
                    found = true;
                }
            }
            
            //if not found check next format from profile
            if (!found) {
                continue;
            }
            

            found = txCount == 0;
            for (int i = 0; i < txCount; i++) {
                if (f.matches(txFormats[i])) {
                    found = true;
                }
            }
            
            //if not found check next format from profile
            if (!found) {
                continue;
            }
            
            payloads[count] = key;
            formats[count++] = f;
            
        }
        return count;
    }
    
    private String createLocalDescriptor() {
        SessionDescriptor sdp = null;
        String userName = "-";

        String sessionID = Long.toString(System.currentTimeMillis() & 0xffffff);
        String sessionVersion = sessionID;

        String networkType = "IN";
        String addressType = "IP4";

        sdp = new SessionDescriptor();
        sdp.createOrigin(userName, sessionID, sessionVersion, networkType, addressType, bindAddress);
        sdp.setSession("Mobicents Media Server");
        sdp.createConnection(networkType, addressType, bindAddress);

        // encode formats
        Collection<MediaType> mediaTypes = ((BaseEndpointImpl)getEndpoint()).getMediaTypes();        
        for (MediaType mediaType : mediaTypes) {
            int i = mediaType.getCode();
            if (formatCount[i] > 0) { 
                RtpSocket rtpSocket = sockets[i];
                int port = rtpSocket.getLocalPort();
                MediaDescriptor md = sdp.addMedia(MediaType.getMediaType(i), port);
            
                for (int k = 0; k < formatCount[i]; k++) {
                    md.addFormat(payloads[i][k], formats[i][k]);
                }
            }
        }
        localDescriptor = sdp.toString();
        return localDescriptor;
    }

    public String getLocalDescriptor() {
        if (getState() == ConnectionState.NULL || getState() == ConnectionState.CLOSED) {
            throw new IllegalStateException("State is " + getState());
        }
        if (this.localDescriptor == null) {
            createLocalDescriptor();
        }
        return this.localDescriptor;
    }

    public String getRemoteDescriptor() {
        return this.remoteDescriptor;
    }

    private InetAddress getAddress(String address) throws UnknownHostException {
        return InetAddress.getByName(address);
    }
    public void setRemoteDescriptor(String descriptor) throws IOException, ResourceUnavailableException {
        //JSR-309 hack
        descriptor = descriptor.replaceAll("16.16.93.241", "127.0.0.1");
        this.remoteDescriptor = descriptor;

        if (getState() != ConnectionState.HALF_OPEN && getState() != ConnectionState.OPEN) {
            throw new IllegalStateException("State is " + getState());
        }


        //parse session descriptor
        SessionDescriptor sdp = new SessionDescriptor(descriptor);
        InetAddress address = getAddress(sdp.getOrigin().getAddress());

        //analyze media descriptions 
        Collection<MediaType> mediaTypes = ((BaseEndpointImpl)getEndpoint()).getMediaTypes();
        //negotiate formats
        for (MediaType mediaType : mediaTypes) {
            subset(sdp.getMediaDescriptor(mediaType.getCode()), mediaType.getCode());
        }
        
        //check format negotiation result
        boolean negotiated = check(sdp);
        if (!negotiated) {
            throw new IOException("Codecs are not negotiated");
        }
        
        //select preffered for each media type
        int count = sdp.getMediaTypeCount();
        for (int i = 0; i < count; i++) {
            MediaDescriptor md = sdp.getMediaDescriptor(i);
            MediaType mediaType = md.getMediaType(); 
            
            //We want to setPeer to only Socket which are bound
        	if((this.stream & mediaType.getMask()) != 0x0){
        		
	            int k = mediaType.getCode();
	            
	            //asign prefferd formats
	            selectPreffered(md);
	            
	            //join sockets and channels
	            sockets[k].setPeer(address, md.getPort());
	            
	            Channel rxChannel = rxChannels[k];
	            Channel txChannel = txChannels[k];
	
	            if (rxChannel != null) {
	                rxChannel.connect(sockets[k].getReceiveStream());
	            }
	            if (txChannel != null) {
	                txChannel.connect(sockets[k].getSendStream());
	            }
	
	            //start transmission
	            sockets[k].getReceiveStream().start();
	            sockets[k].getSendStream().start();
	            
	            setMode(mode[k], mediaType);
        	}
        }
        
        this.localDescriptor = null;
        
        
        setState(ConnectionState.OPEN);
    }

    /**
     * Checks the result of format negotiation.
     * 
     * @return true if all media types are negotiated
     */
    private boolean check(SessionDescriptor sdp) {
        //codecs are negotiated if for all offered media descriptors 
        //the number of formats > 0;
        int count = sdp.getMediaTypeCount();
        boolean passed = true;
        for (int i = 0; i < count; i++) {
            MediaDescriptor md = sdp.getMediaDescriptor(i);
            passed = formatCount[md.getMediaType().getCode()] != 0;
            if (!passed) {
                break;
            }
        }
        return passed;
    }
    
    /**
     * Calculates intersection between local formats and offered
     * 
     * @param md the media descriptor offered.
     * @param mediaType the media type of the offer
     */
    private void subset(MediaDescriptor md, int mediaType) {
        //if media descriptor is null then we set format count to zero
        if (md == null) {
            formatCount[mediaType] = 0;
            return;
        }
        
        //detrmine subset. 
        //the goal is to find only one common media format and rfc2833 event
        
        
        //payload numbers for subset
        int[] pt = new int[15];
        //formats for subset
        Format[] subset = new Format[15];
        
        //the number of formats presented in offer
        int fcount = md.getFormatCount();
        
        //index for subset arrays
        int l = 0;
        
        //flag raised when media formats is found
        boolean mediaFound = false;
        //flag raised when rfc2833 event descriptor is found
        boolean rfc2833Found = false;
        
        //checking offered format in loop
        for (int i = 0; i < fcount; i++) {
            //the next format for analysis.            
            Format f = md.getFormat(i);
            //looking at rfc2833 flag. if format already is found
            //then we skip rfc2833 formats negotiation
            //if not found yet then we compare formats
            //if it matches then search same format in the list of supported
            //if it does not match then check mediaFound flag and if media format is not
            //negotiated yet run loop for searching.
            if (!rfc2833Found && f.matches(AVProfile.DTMF)) {
                for (int k = 0; k < formatCount[mediaType]; k++) {
                    if (formats[mediaType][k].matches(f)) {
                        pt[l] = md.getPyaloadType(i);
                        subset[l++] = f;
                        rfc2833Found = true;                        
                    }
                }
            } else if (!mediaFound) {
                for (int k = 0; k < formatCount[mediaType]; k++) {
                    if (formats[mediaType][k].matches(f)) {
                        pt[l] = md.getPyaloadType(i);
                        subset[l++] = f;
                        mediaFound = true;
                    }
                }
            }
            
            //if all formats negotiated break loop
            if (mediaFound && rfc2833Found) {
                break;
            }
        }
        
        //update supported formats with subset
        this.payloads[mediaType] = pt;
        this.formats[mediaType] = subset;        
        this.formatCount[mediaType] = l;
    }
    
    /**
     * This methods selects preffered format and assigns it to the relative socket
     * 
     * @param md media descriptor.
     */
    private void selectPreffered(MediaDescriptor md) {
        //it takes first non rfc2833 format and assigns it to socket
        int k = md.getMediaType().getCode();
        //formats contains one or two formats. 
        //if two formats then one is RFC2833
        if (formats[k][0].matches(AVProfile.DTMF)) {
            sockets[k].setFormat(payloads[k][1], formats[k][1]);
            sockets[k].setDtmfPayload(payloads[k][0]);
        } else {
            sockets[k].setFormat(payloads[k][0], formats[k][0]);
            sockets[k].setDtmfPayload(payloads[k][1]);
        }
//        for (int i = 0; i < formatCount[k]; i++) {
//            if (!formats[k][i].matches(AVProfile.DTMF)) {
//                sockets[k].setFormat(payloads[k][i], formats[k][i]);
//                break;
//            }
//        }
    }
    
    public void setOtherParty(Connection other) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setOtherParty(Connection other, MediaType mediaType) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
    
    @Override
    public void close(MediaType mediaType){
    	//We want to disconnect only if already joined
    	if((this.stream & mediaType.getMask()) != 0x0){
            int i = mediaType.getCode();
            
            //Reset the FormatCount
            this.formatCount[i] = 0;
            
            if (sockets[i] != null) {
                //terminate transmission
                sockets[i].getReceiveStream().stop();
                sockets[i].getSendStream().stop();
                
                //disconnect rx
                if (rxChannels[i] != null && sockets[i].getReceiveStream().isConnected()) {
                    rxChannels[i].disconnect(sockets[i].getReceiveStream());
                }
                
                //disconnect tx
                if (txChannels[i] != null && sockets[i].getSendStream().isConnected()) {
                    txChannels[i].disconnect(sockets[i].getSendStream());
                }
                
                //disconecting remote side and unbinding from local address
                sockets[i].release();
                
                super.close(mediaType);
                
            }
    	}
    }

    @Override
    public void close() {
        Collection<MediaType> mediaTypes = ((BaseEndpointImpl)getEndpoint()).getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
        	this.close(mediaType);
        }
    }

    public void error(Exception e) {
        //getEndpoint().deleteConnection(this.getId());
    	for (Object ocl : this.connectionListeners.keySet()) {
            ConnectionListener cl = (ConnectionListener) ocl;
            cl.onError(this, e);
        }    	
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsReceived(org.mobicents.media.server.spi.MediaType) 
     */
    public long getPacketsReceived(MediaType media) {
        return sockets[media.getCode()].getReceiveStream().getPacketsTransmitted();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesReceived(org.mobicents.media.server.spi.MediaType) 
     */
    public long getBytesReceived(MediaType media) {
        return sockets[media.getCode()].getReceiveStream().getBytesTransmitted();
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesReceived() 
     */
    public long getBytesReceived() {
        int res = 0;
        for (int i = 0; i < sockets.length; i++) {
            if (sockets[i] != null) {
                res += sockets[i].getBytesReceived();
            }
        }
        return res;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    public long getPacketsTransmitted(MediaType media) {
        return sockets[media.getCode()].getSendStream().getPacketsReceived();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    public long getBytesTransmitted(MediaType media) {
        return sockets[media.getCode()].getSendStream().getBytesReceived();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted() 
     */
    public long getBytesTransmitted() {
        int res = 0;
        for (int i = 0; i < sockets.length; i++) {
            if (sockets[i] != null) {
                res += sockets[i].getBytesReceived();
            }
        }
        return res;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter(org.mobicents.media.server.spi.MediaType) 
     */
    public double getJitter(MediaType media) {
        return sockets[media.getCode()].getJitter();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter() 
     */
    public double getJitter() {
        double res = 0;
        int count = 0;
        for (int i = 0; i < sockets.length; i++) {
            if (sockets[i] != null) {
                res += ((ReceiveStream)sockets[i].getReceiveStream()).getInterArrivalJitter();
                count++;
            }
        }
        return res/count;
    }
    
    public void setOtherParty(String media, InetSocketAddress address) throws IOException {
        //rtpSockets.get(media).setPeer(address.getAddress(), address.getPort());
    }
    
	public void rtcpReceiverTimeout(RtpSocket rtpSocket) {
		//TODO If the Mode of Connection is SEND_RECV or RECV_ONLY and if Receiver Timeout occur's probably
		//the far end died and notify the Connection Listener to take appropriate action
	}

	public void rtcpSenderTimeout(RtpSocket rtpSocket) {
		// TODO If the Mode of Connection is SEND_RECV or SEND_ONLY and no if Sender Timeout occur's 
		// may be the silence started. 
				
	}
	
    @Override
    public String toString() {
        return "RTP Connection [" + getEndpoint().getLocalName() + ", idx=" + getIndex() + "], state=" + getState();
    }	
}
