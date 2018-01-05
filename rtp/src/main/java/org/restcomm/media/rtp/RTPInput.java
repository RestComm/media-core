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

package org.restcomm.media.rtp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Receiver implementation.
 *
 * The Media source of RTP data.
 */
public class RTPInput extends AbstractSource implements BufferListener {
	
	private static final long serialVersionUID = -737259897530641186L;

	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;
    
    //jitter buffer
    private JitterBuffer rxBuffer;
    
	//digital signaling processor
    private Processor dsp;
           
    protected Integer preEvolveCount=0;
    protected Integer evolveCount=0;
    
    private static final Logger logger = LogManager.getLogger(RTPInput.class);
    
    private AudioInput input;
	/**
     * Creates new receiver.
     */
    protected RTPInput(PriorityQueueScheduler scheduler,JitterBuffer jitterBuffer) {
        super("rtpinput", scheduler,PriorityQueueScheduler.INPUT_QUEUE);
        this.rxBuffer=jitterBuffer;        
        input=new AudioInput(1,packetSize);
        this.connect(input);        
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    @Override
    public void reset() {
        super.reset();        
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
    
    protected int getPacketsLost() {
        return 0;
    }    

    @Override
    public Frame evolve(long timestamp) {
    	Frame currFrame=rxBuffer.read(timestamp);
    	
    	if(currFrame!=null)
        {
    		//do the transcoding job
        	if (dsp != null) {
        		try
        		{
        			currFrame = dsp.process(currFrame,currFrame.getFormat(),format);
        		}
        		catch(Exception e)
        		{
        			//transcoding error , print error and try to move to next frame
        			logger.error(e);
        		}
        	}
        	
        }
    	
    	return currFrame; 
    }    
    
    /**
     * RX buffer's call back method.
     * 
     * This method is called when rxBuffer is full and it is time to start
     * transmission to the consumer.
     */
    public void onFill() {
    	this.wakeup();
    }    
}