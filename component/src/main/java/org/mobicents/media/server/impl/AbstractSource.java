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

package org.mobicents.media.server.impl;


import java.util.concurrent.ConcurrentLinkedQueue;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.io.Pipe;
import org.mobicents.media.server.spi.memory.Frame;
import org.apache.log4j.Logger;

/**
 * The base implementation of the Media source.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement general wirring contruct. All media
 * components have to extend one of these classes.
 * 
 * @author Oleg Kulikov
 */
public abstract class AbstractSource extends BaseComponent implements MediaSource {

    //transmission statisctics
    private volatile long txPackets;
    private volatile long txBytes;
    
    //shows if component is started or not.
    private volatile boolean started;

    //stream synchronization flag
    private volatile boolean isSynchronized;

    //local media time
    private volatile long timestamp = 0;
    
    //initial media time
    private long initialOffset;
    
    //frame sequence number
    private long sn = 1;

    //scheduler instance
    private Scheduler scheduler;

    //media generator
    private final Worker worker;

  //duration of media stream in nanoseconds
    protected long duration = -1;

    //intial delay for media processing
    private long initialDelay = 0;
    
    //media transmission pipe
    protected PipeImpl pipe;

    //digital signaling processor
    private Processor dsp;

    //active transmission formats
    private final Formats formats = new Formats();

    //supported formats
    private final Formats supportedFormats = new Formats();

    //temporary buffer
    //private ConcurrentLinkedQueue<Frame> buffer = new ConcurrentLinkedQueue();
    private Stats stats = new Stats();
        
    /**
     * Creates new instance of source with specified name.
     * 
     * @param name
     *            the name of the source to be created.
     */
    public AbstractSource(String name, Scheduler scheduler,int queueNumber) {
        super(name);
        this.scheduler = scheduler;
        this.worker = new Worker(scheduler,queueNumber);
        //this.transcoder = new Transcoder(scheduler);
        this.initFormats();
    }

    /**
     * Initializes list of supported and transmission formats if possible
     */
    private void initFormats() {
        if (this.getNativeFormats().size() > 0) {
            supportedFormats.clean();
            supportedFormats.addAll(getNativeFormats());
        }

        formats.clean();
        formats.addAll(supportedFormats);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.impl.AbstractSource#setInitialDelay(long) 
     */
    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getMediaTime();
     */
    public long getMediaTime() {
        return timestamp;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#setDuration(long duration);
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getDuration();
     */
    public long getDuration() {
        return this.duration;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#setMediaTime(long timestamp);
     */
    public void setMediaTime(long timestamp) {
        this.initialOffset = timestamp;
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
        this.rebuildFormats();
    }
    
    /**
     * Rebuilds the list of supported formats.
     */
    protected void rebuildFormats() {
        //rebuild list of supported formats
        if (dsp == null) {
            this.initFormats();
            return;
        }

        //add formats wich are results of transcoding
        Formats fmts = this.getNativeFormats();
        supportedFormats.clean();
        int fcount = fmts.size();
        for (int i = 0; i < fcount; i++) {
            supportedFormats.add(fmts.get(i));
            for (Codec c : dsp.getCodecs()) {
                if (c.getSupportedInputFormat().matches(fmts.get(i))) {
                    supportedFormats.add(c.getSupportedOutputFormat());
                }
            }
        }

        formats.clean();
        formats.addAll(supportedFormats);

        dsp.setFormats(formats);
    }

    /**
     * Gets the digital signalling processor associated with this media source
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
     * @see org.mobicents.media.MediaSource#setFormats(org.mobicents.media.server.spi.format.Formats)
     */
    public void setFormats(Formats formats) throws FormatNotSupportedException {
        supportedFormats.intersection(formats, this.formats);
        if (dsp != null) {
            dsp.setFormats(this.formats);
        }
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.MediaSource#getFormats()
     */
    public Formats getFormats() {
        return supportedFormats;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#start().
     */
    public void start() {
    	synchronized(worker) {
    		//check scheduler
    		try {
    			//prevent duplicate starting
    			if (started) {
    				return;
    			}

    			if (scheduler == null) {
    				throw new IllegalArgumentException("Scheduler is not assigned");
    			}

    			this.txBytes = 0;
    			this.txPackets = 0;
            
    			//reset media time and sequence number
    			timestamp = this.initialOffset;
    			this.initialOffset = 0;
            
    			sn = 0;

    			//switch indicator that source has been started
    			started = true;

    			//just started component always synchronized as well
    			this.isSynchronized = true;

    			//scheduler worker    
    			worker.reinit();
    			scheduler.submit(worker,worker.getQueueNumber());

    			//started!
    			started();
    		} catch (Exception e) {
    			started = false;
    			failed(e);
    		}
    	}
    }

    /**
     * Restores synchronization
     */
    public void wakeup() {
        synchronized(worker) {
            if (!started) {
                return;
            }
            
            if (!this.isSynchronized) {
                this.isSynchronized = true;
                scheduler.submit(worker,worker.getQueueNumber());
            }
        }
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#stop().
     */
    public void stop() {
        if (started) {
            stopped();
        }
        started = false;
        if (worker != null) {
            worker.cancel();
        }
        timestamp = 0;
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.MediaSource#connect(org.mobicents.media.server.spi.io.Pipe)
     */
    public void connect(Pipe pipe) {
        this.pipe = (PipeImpl) pipe;
        this.pipe.source = this;
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.MediaSource#disconnect(org.mobicents.media.server.spi.io.Pipe)
     */
    public void disconnect(Pipe pipe) {
        if (this.pipe != pipe) {
            throw new IllegalArgumentException(pipe + " is not connected");
        }
        this.pipe.source = null;
        this.pipe = null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isConnected().
     */
    public boolean isConnected() {
        return pipe != null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#isStarted().
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * This method must be overriden by concrete media source. T
     * he media have to fill buffer with media data and
     * attributes.
     * 
     * @param buffer the buffer object for media.
     * @param sequenceNumber
     *            the number of timer ticks from the begining.
     */
    public abstract Frame evolve(long timestamp);

    /**
     * Gets the list of formats supported by this component without possible
     * transcoding.
     * 
     * @return the list of format descriptors
     */
    public abstract Formats getNativeFormats();

    /**
     * Sends notification that media processing has been started.
     */
    protected void started() {
    }

    /**
     * Sends failure notification.
     * 
     * @param e the exception caused failure.
     */
    protected void failed(Exception e) {
    }

    /**
     * Sends notification that signal is completed.
     * 
     */
    protected void completed() {
        this.started = false;
    }

    /**
     * Called when source is stopped by request
     * 
     */
    protected void stopped() {
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getPacketsReceived()
     */
    public long getPacketsTransmitted() {
        return txPackets;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getBytesTransmitted()
     */
    public long getBytesTransmitted() {
        return txBytes;
    }

    @Override
    public void reset() {
        this.txPackets = 0;
        this.txBytes = 0;
        this.formats.clean();
        this.formats.addAll(this.supportedFormats);
    }
    
    public String report() {
        return stats.toString();
    }
    
    
    /* (non-Javadoc)
     * @see org.mobicents.media.MediaSink#getInterface(java.lang.Class)
     */
    @Override
    public <T> T getInterface(Class<T> interfaceType) {
        //should we check default?
        return null;
    }

    /**
     * Media generator task
     */
    private class Worker extends Task {
    	/**
         * Creates new instance of task.
         *
         * @param scheduler the scheduler instance.
         */
    	private int queueNumber;    	
    	private long initialTime;
    	
    	public Worker(Scheduler scheduler,int queueNumber) {
            super(scheduler);
            this.queueNumber=queueNumber;
            initialTime=scheduler.getClock().getTime();            
        }
        
        public void reinit()
        {
        	initialTime=scheduler.getClock().getTime();        	
        }
        
        public int getQueueNumber()
        {
        	return queueNumber;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#perform()
         */
        public long perform() {
        	if(initialDelay+initialTime>scheduler.getClock().getTime())
        	{
        		//not a time yet
        		scheduler.submit(this,queueNumber);
                return 0;
        	}
        	
        	int readCount=0;
        	long overallDelay=0;
        	while(overallDelay<20000000L)
        	{
        		readCount++;
        		Frame frame = evolve(timestamp);
        		if (frame == null) {
        			if(readCount==1)
        			{
        				//stop if frame was not generated
        				isSynchronized = false;
        				return 0;
        			}
        			else
        			{
        				//frame was generated so continue
        				scheduler.submit(this,queueNumber);
        	            return 0;
        			}
            	}

            	//mark frame with media time and sequence number
            	frame.setTimestamp(timestamp);
            	frame.setSequenceNumber(sn);

            	//update media time and sequence number for the next frame
            	timestamp += frame.getDuration();
            	overallDelay += frame.getDuration();
            	sn = inc(sn);

            	//set end_of_media flag if stream has reached the end
            	if (duration > 0 && timestamp >= duration) {
            		frame.setEOM(true);
            	}

            	long frameDuration = frame.getDuration();            	
            	stats.rxFormat = frame.getFormat();
            
          
            	//do the transcoding job
            	if (dsp != null) {
                	frame = dsp.process(frame);
            	}
            
            	stats.txFormat = frame.getFormat();

            	//delivering data to the other party.
            	if (pipe != null) {
                	pipe.write(frame);
            	}

            	//update transmission statistics
            	txPackets++;
            	txBytes += frame.getLength();
            
            	//send notifications about media termination
            	//and do not resubmit this task again if stream has bee ended
            	if (frame.isEOM()) {
            		started = false;
        			completed();
        			return -1;
            	}

            	//check synchronization
            	if (frameDuration <= 0) {
                	//los of synchronization
                	isSynchronized = false;
                	return 0;
            	}            
        	}        
        	
        	scheduler.submit(this,queueNumber);
            return 0;
        }

        @Override
        public String toString() {
            return getName();
        }

    }

    /**
     * Implements DSP and transmission tasks.
     */
    //private class Transcoder extends Task {

        /**
         * Creates new instance of task.
         *
         * @param scheduler the task scheduler.
         */
        /*private Transcoder(Scheduler scheduler) {
            super(scheduler);
        }

        @Override
        public long getPriority() {
            return 0;
        }

        @Override
        public long getDuration() {
            return 0;
        }

        @Override
        public long perform() {
            if (buffer.isEmpty()) {
                //nothing to do
                return 0;
            }
            
            //poll frame from temporary buffer
            Frame frame = buffer.poll();

            //do the transcoding job
            if (dsp != null) {
                frame = dsp.process(frame);
            }
            stats.txFormat = frame.getFormat();

            //delivering data to the other party.
            if (pipe != null) {
                pipe.write(frame);
            }

            //update transmission statistics
            txPackets++;
            txBytes += frame.getLength();

            return 0;
        }

    }*/
    
    private class Stats {
        protected Format rxFormat;
        protected Format txFormat;
        
        @Override
        public String toString() {
            return "source: " + rxFormat + ", codec: " + txFormat;
        }
    }
    
}
