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

package org.restcomm.javax.media.mscontrol.mediagroup.signals.buffer;

/**
 *
 * @author kulikov
 */
public class EventBuffer {
    //string representation of queue;
    private String sequence = "";
    
    //patterns for even detection
    private String patterns[];
    
    //the number of events to detect.
    private int count;
    
    //buffer state listener
    private BufferListener listener;
    
    /**
     * Queues next event to the buffer.
     * 
     * @param event the event
     */
    public void offer(Event event) {
        sequence += event.toString();
        
        //check pattern matching
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
    
    public int length() {
        return sequence.length();
    }
    
    public void flush() {        
        sequence = "";
    }
    
    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }
}
