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

package org.restcomm.media.core.rtp.rfc2833;

import java.io.IOException;

import org.restcomm.media.core.component.AbstractSink;
import org.restcomm.media.core.component.oob.OOBOutput;
import org.restcomm.media.core.rtp.RTPDataChannel;
import org.restcomm.media.core.rtp.RtpTransmitter;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;
import org.restcomm.media.core.spi.memory.Frame;

/**
 * Transmitter implementation.
 * 
 * @author Yulian oifa
 */
public class DtmfOutput extends AbstractSink {

	private static final long serialVersionUID = 1333531209641759516L;

	@Deprecated
    private RTPDataChannel channel;
	private RtpTransmitter transmitter;
    
    private OOBOutput oobOutput;
    
    /**
     * Creates new transmitter
     */
    @Deprecated
    public DtmfOutput(PriorityQueueScheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public DtmfOutput(final PriorityQueueScheduler scheduler,final RtpTransmitter transmitter) {
        super("Output");
        this.transmitter = transmitter;
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
    	
    	if(this.transmitter != null) {
    		this.transmitter.sendDtmf(frame);
    	}
    }            
}
