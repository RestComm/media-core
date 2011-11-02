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

package org.mobicents.media.server.ctrl.mgcp.pkg.au;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;

/**
 *
 * @author kulikov
 */
public class EventBuffer implements DtmfDetectorListener {
    //string representation of queue;
    private String sequence = "";
    
    //patterns for even detection
    private String patterns[];
    
    //the number of events to detect.
    private int count;
    
    //buffer state listener
    private BufferListener listener;
    
    private Logger logger = Logger.getLogger(EventBuffer.class);
    
    public int length() {
        return sequence.length();
    }
    
    public void flush() {        
        sequence = "";
    }
    
    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
        this.sequence = "";
    }
    
    public void setCount(int count) {
        this.count = count;
        this.sequence = "";
    }
    
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }

    public void process(DtmfEvent event) {
        listener.singleTone(event.getTone());
        sequence += event.getTone();

        //check pattern matching for the single symbol
/*        if (patterns != null) {
            for (int i = 0; i < patterns.length; i++) {
                if (s.matches(patterns[i])) {
                    listener.patternMatches(i, sequence);
                    
                    //nullify sequence;
                    sequence = "";
                    return;
                }
            }
        }
*/        
        //check pattern matching for the entire sequence
        if (patterns != null && sequence.length() > 0) {
            for (int i = 0; i < patterns.length; i++) {
                if (sequence.matches(patterns[i])) {
                    listener.patternMatches(i, sequence);
                    
                    //nullify sequence;
                    sequence = "";
                    return;
                }
            }
        }
        
        //check the amount of detected event and notify listener
        //if limit reached.
        if (count > 0 && sequence.length() == count) {
            listener.countMatches(sequence);
            sequence = "";
            count = -1;
        }
    }
}
