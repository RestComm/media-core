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
package org.mobicents.media.server.ivr;

import java.io.IOException;
import java.util.ArrayList;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ConcurrentLinkedList;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.memory.Frame;

/**
 *
 * @author yulian oifa
 */
public class AudioSink extends AbstractSink {

    //local buffer
    private ConcurrentLinkedList<Frame> buffer = new ConcurrentLinkedList();
    
    private SignalSplitter signalSplitter;
    
    private MediaSource dtmfSource;
    //build-in dtmf detector
    private DetectorImpl dtmfDetector;
    //build-in recorder
    private AudioRecorderImpl recorder;
    
    /**
     * Creates new audio announcement source.
     * 
     * @param scheduler the scheduler instance.
     */
    public AudioSink(Scheduler scheduler, String name) {
        super("aap");
        
        //prepare signal mixer
        signalSplitter = new SignalSplitter(scheduler);
        signalSplitter.init();
        
        //construct DTMF detector
        dtmfDetector = new DetectorImpl(name, scheduler);
        dtmfDetector.setVolume(-35);
        dtmfDetector.setDuration(40);

        //fork new output for DTMF
        dtmfSource = signalSplitter.audioSplitter.newOutput();
        
        //join with detector
        dtmfSource.connect(dtmfDetector);
        
        //construct audio recorder
        recorder = new AudioRecorderImpl(scheduler);        
        signalSplitter.add(recorder);                
    }
    
    @Override
    public void start() {
        signalSplitter.start();
        dtmfSource.start();
        super.start();
    }
    
    @Override
    public void stop() {
    	dtmfSource.stop();
        signalSplitter.stop();
        super.stop();
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        buffer.offer(frame);
        signalSplitter.wakeup();
    }
    
    public void add(MediaSink detector) {
        signalSplitter.add(detector);                
    }
    
    private class SignalSplitter extends AbstractSource {
        
        private Splitter audioSplitter;
        private Scheduler scheduler;
        
        public SignalSplitter(Scheduler scheduler) {
            super("signal-mixer", scheduler,scheduler.INPUT_QUEUE);
            this.scheduler = scheduler;            
        }
        
        private void init() {
            audioSplitter = new Splitter(scheduler);            
            this.connect(audioSplitter.getInput());            
            
        }
        
        @Override
        public void start() {
            super.start();
        }
        
        @Override
        public void stop() {
            super.stop();
        }                
        
        public void add(MediaSink detector) {
            MediaSource source = audioSplitter.newOutput();
            
            source.connect(detector);            
            source.start();            
        }

        @Override
        public Frame evolve(long timestamp) {
            return buffer.poll();
        }

    }

    public Component getComponent(ComponentType componentType) {
    	switch(componentType)
    	{
    		case DTMF_DETECTOR:
    			return dtmfDetector; 
    		case RECORDER:
    			return recorder;
    	}
        
        return null;
    }
}
