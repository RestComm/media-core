/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.resource.prelay;

import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.dsp.DspFactory;
import org.mobicents.media.server.impl.dsp.Processor;
import org.mobicents.media.server.impl.resource.cnf.AudioMixer;

/**
 *
 * @author kulikov
 */
public class AudioChannel {
    private Processor p1;
    private Processor p2;
    private AudioMixer mixer;
    
    public AudioChannel(DspFactory dspFactory) {
        p1 = (Processor) dspFactory.newInstance(null);
        p2 = (Processor) dspFactory.newInstance(null);
        mixer = new AudioMixer("PacketRelay");
        
        p1.getOutput().connect(mixer);
        p2.getInput().connect(mixer.getOutput());
    }
    
    public MediaSink getInput() {
        return p1.getInput();
    }
    
    public MediaSource getOutput() {
        return p2.getOutput();
    }
    
    public void connect(MediaSource source) {
        source.connect(p1.getInput());
    }
    
    public void disconnect(MediaSource source) {
        source.disconnect(p1.getInput());
    }
    
    public void connect(MediaSink sink) {
        p2.getOutput().connect(sink);
    }
    
    public void disconnect(MediaSink sink) {
        p2.getOutput().disconnect(sink);
    }
    
    public void stop() {
        mixer.stop();
    }
}
