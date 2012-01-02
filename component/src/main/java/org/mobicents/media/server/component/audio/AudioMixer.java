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

package org.mobicents.media.server.component.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

import org.mobicents.media.server.spi.FormatNotSupportedException;

/**
 * Implements audio mixer.
 * 
 * @author kulikov
 */
public class AudioMixer implements Mixer {
    private final static int POOL_SIZE = 100;

    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //the format of the output stream.
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private Formats formats = new Formats();

    //The pool of input streams
    private ConcurrentLinkedQueue<Input> pool = new ConcurrentLinkedQueue();
    //Active input streams
    private ConcurrentHashMap<Integer,Input> inputs = new ConcurrentHashMap(100);
    
    //output stream
    private final Output output;

    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;

    //frames for mixing
    private Frame[] frames;

    private MixTask mixer;
    private volatile boolean started = false;

    protected long mixCount = 0;
    
    //gain value
    private double gain = 1.0;
    private final Object LOCK = new Object();
    
    public AudioMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        formats.add(format);

        mixer = new MixTask(scheduler);
        output = new Output(scheduler);
        
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.add(new Input(scheduler,(i+1)));
        }
    }

    /**
     * Gets the format of the output stream.
     * 
     * @return the audio format descriptor.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Modifies format of the output stream.
     *
     * @param format the format descriptor.
     */
    public void setFormat(Format format) {
        this.format = (AudioFormat) format;
        packetSize = (int)(period / 1000000) * this.format.getSampleRate()/1000 * this.format.getSampleSize() / 8;
    }

    /**
     * Creates new input for this mixer.
     *
     * @return Input as media sink object.
     */
    public MediaSink newInput() {    	
    	Input input = pool.remove();
    	input.buffer.clear();
    	inputs.put(input.getInputId(),input);
    	return input;    	
    }

    /**
     * Gets the mixed stream.
     *
     * @return the output stream
     */
    public MediaSource getOutput() {
        return output;
    }

    /**
     * Gets the used packet size.
     * 
     * @return the packet size value in bytes.
     */
    protected int getPacketSize() {
        return this.packetSize;
    }

    /**
     * Releases unused input stream
     *
     * @param input the input stream previously created
     */
    public void release(MediaSink input) {
    	((Input)input).recycle();    	
    }

    /**
     * Modify gain of the output stream.
     * 
     * @param gain the new value of the gain in dBm.
     */
    public void setGain(double gain) {
        this.gain = gain > 0 ? gain * 1.26 : gain == 0 ? 1 : 1/(gain * 1.26);
    }
    
    public void start() {
    	synchronized(LOCK) {
/*        	if (!started) {
            	started = true;
            	output.buffer.clear();
            	mixer.setDeadLine(scheduler.getClock().getTime() + 1);
            	scheduler.submit(mixer);
            	mixCount = 0;
	//            output.start();
            	for (Input input: inputs) {
                	input.start();
            	}
        	}
         * 
         */
    		startMixer();
    	}
    }

    public void startMixer() {
        if (!started) {
            started = true;
            output.buffer.clear();
            scheduler.submit(mixer,scheduler.MIXER_MIX_QUEUE);
            mixCount = 0;
            Iterator<Input> activeInputs=inputs.values().iterator();
            while(activeInputs.hasNext())
            	activeInputs.next().start();            
        }
    }
    
    public void stop() {
    	synchronized(LOCK) {
//        	started = false;
//        	mixer.cancel();
//        	output.stop();
//        	for (Input input: inputs) input.stop();    		
    		stopMixer(true);
    	}
    }

    public void stopMixer(boolean stopOutput) {
        started = false;
        mixer.cancel();
        if (stopOutput) {
            output.stop();
        }
        
        Iterator<Input> activeInputs=inputs.values().iterator();
        while(activeInputs.hasNext())
        	activeInputs.next().stop();         
    }
    
    public String report() {
        StringBuilder builder = new StringBuilder();
        builder.append("Mixer{\n");
        
        builder.append("     output:  (");
        builder.append(output.report());
        builder.append(")\n");
        
        Iterator<Input> activeInputs=inputs.values().iterator();
        while(activeInputs.hasNext())
        {
        	Input input=activeInputs.next();
            builder.append("     input: (");
            builder.append(input.report());
            builder.append(")\n");
        }
        
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * Input stream
     */
    private class Input extends AbstractSink {
        private int inputId;
        //50 frames is too much , 5 frames equals 100ms
        private int limit=5;
        private ConcurrentLinkedQueue<Frame> buffer = new ConcurrentLinkedQueue();
        private Frame activeFrame=null;
        private byte[] activeData;
        private int byteIndex=0;
        
        //private ElasticBuffer buffer = new ElasticBuffer(3, 10);

        /**
         * Creates new stream
         */
        public Input(Scheduler scheduler,int inputId) {
            super("mixer.input", scheduler,scheduler.MIXER_INPUT_QUEUE);
            this.inputId=inputId;
        }
        
        public int getInputId()
        {
        	return inputId;
        }
        
        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
        	//generate frames with correct size here , aggregate frames if needed.
        	//allows to accept several sources with different ptime ( packet time ) 
        	if (buffer.size() >= limit) 
        		buffer.remove();
            
        	byte[] oldData=frame.getData();        	
        	for(int i=0;i<oldData.length;i++)        	
        	{
        		if(activeData==null)
        		{
        			activeFrame=Memory.allocate(packetSize);
        			activeFrame.setOffset(0);
        			activeFrame.setLength(packetSize);
        			activeData=activeFrame.getData(); 
        			byteIndex=0;
        		}
        		
        		activeData[byteIndex++]=oldData[i];
        		
        		if(byteIndex>=activeData.length)
        		{
        			buffer.offer(activeFrame);
        			activeFrame=null;
        			activeData=null;
        		}
        	}
        	
        	frame.recycle();
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.impl.AbstractSink#getNativeFormats() 
         */
        public Formats getNativeFormats() {
            return formats;
        }

        /**
         * Indicates the state of the input buffer.
         *
         * @return true if input buffer has no frames.
         */
        protected boolean isEmpty() {
            return buffer.isEmpty();
        }

        /**
         * Retrieves frame from the input buffer.
         *
         * @return the media frame.
         */
        protected Frame poll() {
//            return buffer.read();
            return buffer.poll();
        }

        /**
         * Recycles output stream
         */
        protected void recycle() {
            buffer.clear();
            activeFrame=null;
			activeData=null;
			byteIndex=0;
			
            inputs.remove(this.inputId);
            pool.add(this);                        
        }

    }

    /**
     * Output stream
     */
    private class Output extends AbstractSource {
        private ConcurrentLinkedQueue<Frame> buffer = new ConcurrentLinkedQueue();
        /**
         * Creates new instance with default name.
         */
        public Output(Scheduler scheduler) {
            super("mixer.output", scheduler,scheduler.MIXER_OUTPUT_QUEUE);
        }

        @Override
        public Frame evolve(long timestamp) {
        	return buffer.poll();
        }
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.impl.AbstractSource#getNativeFormats()
         */
        public Formats getNativeFormats() {
            return formats;
        }

        @Override
        public void start() {
            startMixer();
            super.start();            
        }
        
        @Override
        public void stop() {        	
        	//System.out.println("MIX COUNT:" + mixCount);    
        	stopMixer(false);
            super.stop();            
        }
    }

    private class MixTask extends Task {

        private volatile long priority;
        private volatile long duration = -100;

        public MixTask(Scheduler scheduler) {
            super(scheduler);
        }
        
        public long getPriority() {
            return priority;
        }

        public void setPriority(long priority) {
            this.priority = priority;
        }

        public long getDuration() {
            return duration;
        }

        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
            try {
            //allocate new frame
            Frame frame = Memory.allocate(packetSize);
            byte[] data = frame.getData();

            //poll inputs
            int count=0;
            frames = new Frame[inputs.size()];
            Iterator<Input> activeInputs=inputs.values().iterator();
            while(activeInputs.hasNext())
            {
            	Input input=activeInputs.next();
            	frames[count] = input.poll();
            	if(frames[count]!=null && frames[count].getLength()!=packetSize)
                {            		
            		//may happen after codec changes
                	frames[count].recycle();
                	frames[count]=null;                	
                }
                
                if(frames[count]!=null)
                	count++;
            }                        

            if(count==0)
            {            	
            	frame.recycle();
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            	return 0;
            }

            //do mixing
        	int k = 0;
        	for (int j = 0; j < packetSize; j += 2) {
        		short s = 0;
        		for (int i = 0; i < count; i++) {
        			s += (short) (((frames[i].getData()[j + 1]) << 8) | (frames[i].getData()[j] & 0xff));                    
        		}
            
        		s = (short)Math.round((double) s * gain);
            
        		data[k++] = (byte) (s);
        		data[k++] = (byte) (s >> 8);
        	}
        	
        	//recycle received frames
        	for (int i = 0; i < count; i++) {
        		//we are generating new frames , why should we send old headers????
        		/*if (frames[i].getHeader() != null) {
        			frame.setHeader(frames[i].getHeader());
        		}*/
        		frames[i].recycle();                
        	}
        	
            //assign attributes to the new frame
            frame.setOffset(0);
            frame.setLength(packetSize);
            frame.setDuration(period);
            frame.setFormat(format);

            //finita la comedia
            output.buffer.offer(frame);
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            output.wakeup();

            mixCount++;

            return 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

}
