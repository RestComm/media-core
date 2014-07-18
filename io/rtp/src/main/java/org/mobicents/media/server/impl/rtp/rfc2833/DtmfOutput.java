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

package org.mobicents.media.server.impl.rtp.rfc2833;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.impl.rtp.RtpHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
/**
 *
 * @author Yulian oifa
 */
/**
 * Transmitter implementation.
 *
 */
public class DtmfOutput extends AbstractSink {
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
	
	@Deprecated
    private RTPDataChannel channel;
	private RtpHandler rtpHandler;
    
    private static final Logger logger = Logger.getLogger(DtmfOutput.class);
    
    private OOBOutput oobOutput;
    
    /**
     * Creates new transmitter
     */
    @Deprecated
    public DtmfOutput(Scheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public DtmfOutput(final Scheduler scheduler,final RtpHandler rtpHandler) {
        super("Output");
        this.rtpHandler = rtpHandler;
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public OOBOutput getOOBOutput()
    {
    	return this.oobOutput;
    }
    
    public void activate()
    {
    	oobOutput.start();
    }
    
    public void deactivate()
    {
    	oobOutput.stop();
    }    
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	// TODO deprecated - hrosa
    	if(this.channel != null) {
    		channel.sendDtmf(frame);
    	}
    	
    	if(this.rtpHandler != null) {
    		this.rtpHandler.sendDtmf(frame);
    	}
    }            
}
