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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.AudioOutput;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
/**
 *
 * @author Yulian oifa
 */
/**
 * Transmitter implementation.
 *
 */
public class RTPOutput extends AbstractSink {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	
	@Deprecated
    private RTPDataChannel channel;
	
	private RtpHandler rtpHandler;
    
    //active formats
    private Formats formats;
    
    //signaling processor
    private Processor dsp;                               
        
    private static final Logger logger = Logger.getLogger(RTPOutput.class);
    
    private AudioOutput output;
    
    /**
     * Creates new transmitter
     */
    @Deprecated
    protected RTPOutput(Scheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        output=new AudioOutput(scheduler,1);
        output.join(this);        
    }

    protected RTPOutput(Scheduler scheduler,RtpHandler rtpHandler) {
    	super("Output");
    	this.rtpHandler = rtpHandler;
    	output=new AudioOutput(scheduler,1);
    	output.join(this);        
    }
    
    public AudioOutput getAudioOutput()
    {
    	return this.output;
    }
    
    public void activate()
    {
    	output.start();
    }
    
    public void deactivate()
    {
    	output.stop();
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        //assign processor
        this.dsp = dsp;        
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public Processor getDsp() {
        return this.dsp;
    }

    /**
     * (Non Java-doc.)
     *
     *
     * @see org.mobicents.media.MediaSink#setFormats(org.mobicents.media.server.spi.format.Formats)
     */
    public void setFormats(Formats formats) throws FormatNotSupportedException {
    	this.formats=formats;    	
    }       

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	//do transcoding
    	if (dsp != null && formats!=null && !formats.isEmpty()) {
    		try
    		{
    			frame = dsp.process(frame,format,formats.get(0));            			
    		}
    		catch(Exception e)
    		{
    			//transcoding error , print error and try to move to next frame
    			logger.error(e);
    			return;
    		} 
    	}

    	// XXX deprecated code
    	if(this.channel != null) {
    		channel.send(frame);
    	}
    	
    	if(this.rtpHandler != null) {
    		this.rtpHandler.send(frame);
    	}
    }            
}
