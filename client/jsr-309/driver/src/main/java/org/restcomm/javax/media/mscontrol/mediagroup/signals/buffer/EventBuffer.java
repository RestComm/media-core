/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
