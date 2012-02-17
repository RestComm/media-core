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
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ConcurrentLinkedList;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.memory.Frame;

/**
 *
 * @author kulikov
 */
public class AudioSink extends AbstractSink {

    //local buffer
    private ConcurrentLinkedList<Frame> buffer = new ConcurrentLinkedList();
    
    private SignalSplitter signalSplitter;
    private ArrayList<Component> components = new ArrayList();
    
    //build-in dtmf detector and pipe for connection
    private DetectorImpl dtmfDetector;
    private PipeImpl dtmfPipe;
    
    /**
     * Creates new audio announcement source.
     * 
     * @param scheduler the scheduler instance.
     */
    public AudioSink(Scheduler scheduler, String name) {
        super("aap", scheduler,scheduler.RX_TASK_QUEUE);
        
        //prepare signal mixer
        signalSplitter = new SignalSplitter(scheduler);
        signalSplitter.init();
        
        //construct DTMF detector
        dtmfDetector = new DetectorImpl(name, scheduler);
        dtmfDetector.setVolume(-35);
        dtmfDetector.setDuration(40);
        
        //attach DTMF detector
        dtmfPipe = new PipeImpl();

        //fork new output for DTMF
        MediaSource dtmfSource = signalSplitter.audioSplitter.newOutput();
        
        //join with detector
        dtmfPipe.connect(dtmfSource);
        dtmfPipe.connect(dtmfDetector);
    }
    
    @Override
    public void start() {
        signalSplitter.start();
        dtmfPipe.start();
        super.start();
    }
    
    @Override
    public void stop() {
        dtmfPipe.stop();
        signalSplitter.stop();
        super.stop();
    }
    
    public void add(MediaSink detector) {
        signalSplitter.add(detector);
    }
        
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        buffer.offer(frame);
        signalSplitter.wakeup();
    }
    
    private class SignalSplitter extends AbstractSource {
        
        private Splitter audioSplitter;
        private PipeImpl pipe1;
        private Scheduler scheduler;
        
        public SignalSplitter(Scheduler scheduler) {
            super("signal-mixer", scheduler,scheduler.INPUT_QUEUE);
            this.scheduler = scheduler;            
        }
        
        private void init() {
            pipe1 = new PipeImpl();            
            audioSplitter = new Splitter(scheduler);
            
            pipe1.connect(audioSplitter.getInput());
            pipe1.connect(this);
            
        }
        
        @Override
        public void start() {
            audioSplitter.getInput().start();
            super.start();
        }
        
        @Override
        public void stop() {
            audioSplitter.getInput().stop();
            super.stop();
        }                
        
        public void add(MediaSink detector) {
            MediaSource source = audioSplitter.newOutput();
            
            PipeImpl p = new PipeImpl();
            p.connect(source);
            p.connect(detector);
            
            source.start();
            components.add(detector);
        }

        @Override
        public Frame evolve(long timestamp) {
            return buffer.poll();
        }

    }

    public Component getComponent(Class intf) {
        if (intf.equals(DtmfDetector.class)) {
            return dtmfDetector;            
        }
        
        for (int i = 0; i < components.size(); i++) {
            if ((components.get(i).getClass() == intf) ||
                (components.get(i).getInterface(intf) != null)) {
                return components.get(i);
            }
        }
        return null;
    }
}
