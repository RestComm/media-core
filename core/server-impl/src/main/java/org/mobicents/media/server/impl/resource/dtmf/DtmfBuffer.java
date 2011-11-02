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
package org.mobicents.media.server.impl.resource.dtmf;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;

/**
 * Implements digit buffer.
 * 
 * @author Oleg Kulikov
 */
public class DtmfBuffer implements Serializable {

    public int interdigitInterval = 500;
    
    private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue();
    private int size = 20;
    
    private long lastActivity = System.currentTimeMillis();
    private String lastSymbol;

    private DetectorImpl detector;
    private Logger logger = Logger.getLogger(DtmfBuffer.class);
    
    public DtmfBuffer(DetectorImpl detector) {
        this.detector = detector;
    }

    public void setInterdigitInterval(int silence) {
        this.interdigitInterval = silence;
    }

    public int getInterdigitInterval() {
        return this.interdigitInterval;
    }

    public void push(String symbol) {
        long now = System.currentTimeMillis();
        if (!symbol.equals(lastSymbol) || (now - lastActivity > interdigitInterval)) {
            if (queue.size() == size) {
                queue.poll();
            }
            queue.offer(symbol);
            
            lastActivity = now;
            lastSymbol = symbol;
            
            logger.info("Send " + symbol);
            detector.fireEvent(symbol);
        }
    }
    
    public void flush() {
        queue.clear();
    }    
}
