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

package org.mobicents.media.server.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import org.mobicents.media.server.SdpTemplate;

import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionFailureListener;
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
 * @author kulikov
 * @author amit bhayani
 */
public class RtpConnectionImpl extends BaseConnection implements RTPChannelListener {	
	private final static AudioFormat DTMF = FormatFactory.createAudioFormat("telephone-event", 8000);
	private final static AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
    
	private RTPDataChannel rtpAudioChannel;
    private RTPDataChannel rtpVideoChannel;

    private SessionDescription sdp = new SessionDescription();
    private SdpComparator sdpComparator = new SdpComparator();

    private boolean isAudioCapabale;
    private boolean isVideoCapable;

    protected RTPFormats audioFormats;
    protected RTPFormats videoFormats;
    
    //SDP template
    protected SdpTemplate template;
    protected String descriptor;    
    
    //negotiated sdp
    private String descriptor2;
        
    private ConnectionFailureListener connectionFailureListener;
    
    public RtpConnectionImpl(String id, Connections connections,Boolean isLocalToRemote) throws Exception {
        super(id, connections,isLocalToRemote);

        //check endpoint capabilities
        this.isAudioCapabale =
                connections.endpoint.getSource(MediaType.AUDIO) != null ||
                connections.endpoint.getSink(MediaType.AUDIO) != null;

        this.isVideoCapable =
                connections.endpoint.getSource(MediaType.VIDEO) != null ||
                connections.endpoint.getSink(MediaType.VIDEO) != null;

        //create audio and video channel
        rtpAudioChannel = connections.rtpManager.getChannel();
        rtpVideoChannel = connections.rtpManager.getChannel();
        rtpAudioChannel.setRtpChannelListener(this);
        rtpVideoChannel.setRtpChannelListener(this);
        
        if (connections.dspFactory != null) {
        	rtpAudioChannel.getInput().setDsp(connections.dspFactory.newProcessor());
        	rtpAudioChannel.getOutput().setDsp(connections.dspFactory.newProcessor());        	
        }
        
        //create sdp template
        audioFormats = getRTPMap(audioChannel, AVProfile.audio);
        videoFormats = getRTPMap(videoChannel, AVProfile.video);

        template = new SdpTemplate(audioFormats, videoFormats);
    }

    /**
     * Constructs RTP payloads for given channel.
     *
     * @param channel the media channel
     * @param profile AVProfile part for media type of given channel
     * @return collection of RTP formats.
     */
    private RTPFormats getRTPMap(Channel channel, RTPFormats profile) {
        RTPFormats list = new RTPFormats();
        Formats fmts=null;
        switch(channel.getMediaType())
        {
        	case AUDIO:
        		fmts=new Formats();
        		if(rtpAudioChannel.getOutput().getDsp()!=null)
        		{
        			Codec[] currCodecs=rtpAudioChannel.getOutput().getDsp().getCodecs();
        			for(int i=0;i<currCodecs.length;i++)
        				if(currCodecs[i].getSupportedInputFormat().matches(format))
        					fmts.add(currCodecs[i].getSupportedOutputFormat());
        		}
        		
        		fmts.add(DTMF);
        		break;
        	case VIDEO:
        		break;
        }
        
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
        if (!(other instanceof RtpConnectionImpl)) {
            throw new IOException("Incompatible connections");
        }

        RtpConnectionImpl otherConnection = (RtpConnectionImpl) other;
        try {
            setOtherParty(otherConnection.getEndpoint().describe(MediaType.AUDIO).getBytes());
        } catch (Exception e) {
        	throw new IOException(e.getMessage());
        }
    }

    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException  {    	
    	super.setMode(mode);
    	rtpAudioChannel.updateMode(mode);    
    	//rtpVideoChannel.updateMode(mode);
    }
    
    public void setOtherParty(byte[] descriptor) throws IOException {
        try {
            sdp.parse(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        sdpComparator.negotiate(sdp, this.audioFormats, this.videoFormats);

        RTPFormats audio = sdpComparator.getAudio();
        RTPFormats video = sdpComparator.getVideo();
        if ((audio.isEmpty() || !audio.hasNonDTMF()) && video.isEmpty()) {
            throw new IOException("Codecs are not negotiated");
        }

        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audioFormats);
            try {
            	rtpAudioChannel.getOutput().setFormats(audio.getFormats());            	
            }
        	catch (FormatNotSupportedException e) {
            //never happen
        		e.printStackTrace();
        	}
        }

        if (!video.isEmpty()) {
            rtpVideoChannel.setFormatMap(videoFormats);
            try {
            	rtpVideoChannel.getOutput().setFormats(video.getFormats());
            }
        	catch (FormatNotSupportedException e) {
            //never happen
        	}            
        }

        String address = null;
        if (sdp.getConnection() != null) {
            address = sdp.getConnection().getAddress();
        }

        if (sdp.getAudioDescriptor() != null) {
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if (sdp.getVideoDescriptor() != null) {
            rtpVideoChannel.setPeer(new InetSocketAddress(address,sdp.getVideoDescriptor().getPort()));
        }
        

        descriptor2 = new SdpTemplate(audio, video).getSDP(connections.rtpManager.getBindAddress(),
                "IN", "IP4",
                connections.rtpManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),
                rtpVideoChannel.getLocalPort());
        try {
            this.join();
        } catch (Exception e) {
        }
    }

    public void setOtherParty(Text descriptor) throws IOException {
    	try {
            sdp.init(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        if (sdp != null && sdp.getAudioDescriptor() != null && sdp.getAudioDescriptor().getFormats() != null) {
            System.out.println("Formats" + sdp.getAudioDescriptor().getFormats());
        }
        sdpComparator.negotiate(sdp, this.audioFormats, this.videoFormats);

        RTPFormats audio = sdpComparator.getAudio();
        RTPFormats video = sdpComparator.getVideo();
        if ((audio.isEmpty() || !audio.hasNonDTMF()) && video.isEmpty()) {
            throw new IOException("Codecs are not negotiated");
        }

        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audioFormats);
            try {
            	rtpAudioChannel.getOutput().setFormats(audio.getFormats());            	
            }
        	catch (FormatNotSupportedException e) {
            //never happen
        		e.printStackTrace();
        	}
        }

        if (!video.isEmpty()) {
            rtpVideoChannel.setFormatMap(videoFormats);
            try {
            	rtpVideoChannel.getOutput().setFormats(video.getFormats());
            }
        	catch (FormatNotSupportedException e) {
            //never happen
        	}            
            //videoChannel.selectFormats(video.getFormats());
        }

        String address = null;
        if (sdp.getAudioDescriptor() != null) {
            address = sdp.getAudioDescriptor().getConnection() != null? 
                    sdp.getAudioDescriptor().getConnection().getAddress() :
                    sdp.getConnection().getAddress();
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if (sdp.getVideoDescriptor() != null) {
            address = sdp.getVideoDescriptor().getConnection() != null? 
                    sdp.getVideoDescriptor().getConnection().getAddress() :
                    sdp.getConnection().getAddress();
            rtpVideoChannel.setPeer(new InetSocketAddress(address,sdp.getVideoDescriptor().getPort()));
        }

        descriptor2 = new SdpTemplate(audio, video).getSDP(connections.rtpManager.getBindAddress(),
                "IN", "IP4",
                connections.rtpManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),
                rtpVideoChannel.getLocalPort());
        
        try {
            this.join();
        } catch (Exception e) {
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
    
    @Override
    public long getPacketsReceived(MediaType media) {
        return 0;
    }

    @Override
    public long getBytesReceived(MediaType media) {
        return 0;
    }
    
    @Override
    public long getBytesReceived() {
        return 0;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    @Override
    public long getPacketsTransmitted(MediaType media) {
        return 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    @Override
    public long getBytesTransmitted(MediaType media) {
        return 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted() 
     */
    @Override
    public long getBytesTransmitted() {
        return 0;
    }
    
    @Override
    public double getJitter(MediaType media) {
        return 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter() 
     */
    @Override
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
        if (this.isAudioCapabale) {
            rtpAudioChannel.bind();
            audioChannel.connect(rtpAudioChannel);
        }

        if (this.isVideoCapable) {
            rtpVideoChannel.bind();
            videoChannel.connect(rtpVideoChannel);
        }

        descriptor = template.getSDP(connections.rtpManager.getBindAddress(),
                "IN", "IP4",
                connections.rtpManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),
                rtpVideoChannel.getLocalPort());
    }    
    
    @Override
    protected void onFailed() {
    	if(this.connectionFailureListener!=null)
        	connectionFailureListener.onFailure();
        
        if (rtpAudioChannel != null) {        	
        	audioChannel.disconnect();
            rtpAudioChannel.close();
        }

        if (rtpVideoChannel != null) {
        	videoChannel.disconnect();
            rtpVideoChannel.close();
        }
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

        if (this.isAudioCapabale) {
        	audioChannel.disconnect();
            this.rtpAudioChannel.close();
        }

        if (this.isVideoCapable) {
        	videoChannel.disconnect();
            this.rtpVideoChannel.close();
        }

        connections.releaseConnection(this,ConnectionType.RTP);        
        this.connectionFailureListener=null;          
    }
}
