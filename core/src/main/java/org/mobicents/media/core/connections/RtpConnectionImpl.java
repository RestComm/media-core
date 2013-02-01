/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.core.connections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;

import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.core.SdpTemplate;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.impl.rtp.RTPChannelListener;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.SdpComparator;
import org.mobicents.media.server.impl.rtp.sdp.SessionDescription;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.utils.Text;

/**
 * 
 * @author Oifa Yulian
 * @author amit bhayani
 */
public class RtpConnectionImpl extends BaseConnection implements RTPChannelListener {	
	private final static AudioFormat DTMF = FormatFactory.createAudioFormat("telephone-event", 8000);
	private final static AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
    
	private RTPDataChannel rtpAudioChannel;
    
    private SessionDescription sdp = new SessionDescription();
    private SdpComparator sdpComparator = new SdpComparator();

    private boolean isAudioCapabale;
    
    protected RTPFormats audioFormats;
    
    //SDP template
    protected SdpTemplate template;
    protected String descriptor;    
    
    //negotiated sdp
    private String descriptor2;
    private boolean isLocal=false;
    private ConnectionFailureListener connectionFailureListener;
    
    private static final Logger logger = Logger.getLogger(RtpConnectionImpl.class);
    
    private ChannelsManager channelsManager;
    
    public RtpConnectionImpl(int id,ChannelsManager channelsManager,DspFactory dspFactory)
    {
        super(id,channelsManager.getScheduler());

        this.channelsManager=channelsManager;
        //check endpoint capabilities
        this.isAudioCapabale =true;                

        //create audio and video channel
        rtpAudioChannel = channelsManager.getChannel();
        
        try {
        	rtpAudioChannel.setInputDsp(dspFactory.newProcessor());
        	rtpAudioChannel.setOutputDsp(dspFactory.newProcessor());
        }
        catch(Exception e) {
        	//exception may happen only if invalid classes have been set in config
        }
        
        //create sdp template
        audioFormats = getRTPMap(AVProfile.audio);
        
        template = new SdpTemplate(audioFormats,null);
    }

    public AudioComponent getAudioComponent()
    {
    	return this.rtpAudioChannel.getAudioComponent();
    }
    
    public OOBComponent getOOBComponent()
    {
    	return this.rtpAudioChannel.getOOBComponent();
    }
    
    /**
     * Gets whether connection should be bound to local or remote interface , supported only for rtp connections.
     *
     * @return boolean value
     */
    @Override
    public boolean getIsLocal()
    {
    	return this.isLocal;
    }
    
    /**
     * Gets whether connection should be bound to local or remote interface , supported only for rtp connections.
     *
     * @return boolean value
     */
    @Override
    public void setIsLocal(boolean isLocal)
    {
    	this.isLocal=isLocal;
    }
    
    /**
     * Constructs RTP payloads for given channel.
     *
     * @param channel the media channel
     * @param profile AVProfile part for media type of given channel
     * @return collection of RTP formats.
     */
    private RTPFormats getRTPMap(RTPFormats profile) {
        RTPFormats list = new RTPFormats();
        Formats fmts=new Formats();
		if(rtpAudioChannel.getOutputDsp()!=null)
		{
			Codec[] currCodecs=rtpAudioChannel.getOutputDsp().getCodecs();
			for(int i=0;i<currCodecs.length;i++)
				if(currCodecs[i].getSupportedInputFormat().matches(format))
					fmts.add(currCodecs[i].getSupportedOutputFormat());
		}
		
		fmts.add(DTMF);
        
        if(fmts!=null)
        {
        	for (int i = 0; i < fmts.size(); i++) {
        		RTPFormat f = profile.find(fmts.get(i));
        		if (f != null) list.add(f.clone());
        	}
        }
        
        return list;
    }
    
    @Override
    public void setOtherParty(Connection other) throws IOException {
        throw new IOException("Applicable on locale connections only");        
    }

    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException  {    	
    	rtpAudioChannel.updateMode(mode);     	
    	super.setMode(mode);
    }
    
    public void setOtherParty(byte[] descriptor) throws IOException {
        try {
            sdp.parse(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        sdpComparator.negotiate(sdp, this.audioFormats, null);

        RTPFormats audio = sdpComparator.getAudio();
        if (audio.isEmpty() || !audio.hasNonDTMF()) {
            throw new IOException("Codecs are not negotiated");
        }
        
        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audioFormats);
            try {
            	rtpAudioChannel.setOutputFormats(audio.getFormats());            	
            }
        	catch (FormatNotSupportedException e) {
        		//never happen
        		throw new IOException(e);
        	}
        }

        String address = null;
        if (sdp.getConnection() != null) {
            address = sdp.getConnection().getAddress();
        }

        if (sdp.getAudioDescriptor() != null) {
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if(!isLocal)
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
        try {
            this.join();
        } catch (Exception e) {
        	//exception is possible here when already joined , should not log
        }
    }

    public void setOtherParty(Text descriptor) throws IOException {
    	try {
            sdp.init(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        if (sdp != null && sdp.getAudioDescriptor() != null && sdp.getAudioDescriptor().getFormats() != null) {
            logger.info("Formats" + sdp.getAudioDescriptor().getFormats());
        }
        sdpComparator.negotiate(sdp, this.audioFormats, null);

        RTPFormats audio = sdpComparator.getAudio();
        if (audio.isEmpty() || !audio.hasNonDTMF()) {
            throw new IOException("Codecs are not negotiated");
        }
        
        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audioFormats);
            try {
            	rtpAudioChannel.setOutputFormats(audio.getFormats());            	
            }
        	catch (FormatNotSupportedException e) {
        		//never happen
        		throw new IOException(e);
        	}
        }

        String address = null;
        if (sdp.getAudioDescriptor() != null) {
            address = sdp.getAudioDescriptor().getConnection() != null? 
                    sdp.getAudioDescriptor().getConnection().getAddress() :
                    sdp.getConnection().getAddress();
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if(!isLocal)
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
        try {
            this.join();
        } catch (Exception e) {
        	//exception is possible here when already joined , should not log
        }                      
    }
    

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getDescriptor()
     */
    @Override
    public String getDescriptor() {
        return descriptor2 != null ? descriptor2 : descriptor;
    }
    
    public long getPacketsReceived() {
    	return rtpAudioChannel.getPacketsReceived();       	
    }

    public long getBytesReceived() {
        return 0;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    public long getPacketsTransmitted() {
    	return rtpAudioChannel.getPacketsTransmitted();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted() 
     */
    public long getBytesTransmitted() {
        return 0;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter() 
     */
    public double getJitter() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "RTP Connection [" + getEndpoint().getLocalName() ;
    }

    public void onRtpFailure() {
    	onFailed();
    }
    
    @Override
    public void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener)
    {
    	this.connectionFailureListener=connectionFailureListener;
    }
    
    @Override
    protected void onCreated() throws Exception {
        if (this.isAudioCapabale)
            rtpAudioChannel.bind(isLocal);        

        if(!isLocal)
        	descriptor = template.getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor = template.getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
    }    
    
    @Override
    protected void onFailed() {
    	if(this.connectionFailureListener!=null)
        	connectionFailureListener.onFailure();
        
        if (rtpAudioChannel != null)        	
            rtpAudioChannel.close();        
    }

    @Override
    protected void onOpened() throws Exception {
    }

    @Override
    protected void onClosed() {
        descriptor2 = null;
        
        try {
            setMode(ConnectionMode.INACTIVE);
        } catch (ModeNotSupportedException e) {
        }

        if (this.isAudioCapabale)
        	this.rtpAudioChannel.close();        

        releaseConnection(ConnectionType.RTP);        
        this.connectionFailureListener=null;          
    }
}
