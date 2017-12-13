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

package org.restcomm.media.sdp.format;

import java.util.ArrayList;

import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.Formats;
/**
 * Implements RTP formats collection with fast search.
 *
 * We assume that RTP formats collection varies slow.
 * @author kulikov
 */
public class RTPFormats {
    //the default size of this collection
    private final static int size = 10;

    //backing array
    private ArrayList<RTPFormat> rtpFormats;
    
    private Formats formats = new Formats();

    private int cursor;
    
    /**
     * Creates new format collection with default size.
     */
    public RTPFormats() {
        this.rtpFormats = new ArrayList<RTPFormat>(size);
    }

    public int getLen()
    {
    	return this.rtpFormats.size();
    }
    /**
     * Creates new formats collection with specified size
     *
     * @param size the size of collection to be created.
     */
    public RTPFormats(int size) {
        this.rtpFormats = new ArrayList<RTPFormat>(size);
    }

    public void add(RTPFormat rtpFormat) {
        rtpFormats.add(rtpFormat);
        formats.add(rtpFormat.getFormat());
    }

    public void add(RTPFormats fmts) {
        for (int i = 0; i < fmts.rtpFormats.size(); i++) {
            rtpFormats.add(fmts.rtpFormats.get(i));
            formats.add(fmts.rtpFormats.get(i).getFormat());
        }
    }
    
    public void remove(RTPFormat rtpFormat) {
        int pos = -1;
        for (int i = 0; i < rtpFormats.size(); i++) {
            pos++;
            if (rtpFormats.get(i).getID() == rtpFormat.getID()) break;
        }

        if (pos == -1) {
            throw new IllegalArgumentException("Unknown format " + rtpFormat);
        }

        rtpFormats.remove(pos);
        formats.remove(rtpFormat.getFormat());
    }

    public void clean() {
    	rtpFormats.clear();
        formats.clean();
        cursor = 0;
    }

    public int size() {
        return rtpFormats.size();
    }
    
    public RTPFormat getRTPFormat(int payload) {
        for (int i = 0; i < rtpFormats.size(); i++) {
            if (rtpFormats.get(i).getID() == payload) return rtpFormats.get(i);
        }
        return null;
    }

    public RTPFormat getRTPFormat(String name) {
        final int size = rtpFormats.size();
        for (int i = 0; i < size; i++) {
            final RTPFormat rtpFormat = rtpFormats.get(i);
            if(rtpFormat.getFormat().getName().toString().equalsIgnoreCase(name)) {
                return rtpFormat;
            }
        }
        return null;
    }

    public RTPFormat getRTPFormat(Format format) {
        for (int i = 0; i < rtpFormats.size(); i++) {
            if (rtpFormats.get(i).getFormat().matches(format)) return rtpFormats.get(i);
        }
        return null;
    }

    public RTPFormat[] toArray() {
        RTPFormat[] fmts = new RTPFormat[rtpFormats.size()];
        return rtpFormats.toArray(fmts);        
    }

    public Formats getFormats() {
        return formats;
    }
    
    public RTPFormat find(int p) {
    	int size = rtpFormats.size();
        for (int i = 0; i < size; i++) {
        	if (rtpFormats.get(i).getID() == p) {
        		return rtpFormats.get(i);
        	}
        }
        return null;
    }
    
    public boolean contains(int p) {
    	return this.find(p) != null;
    }
    
    public boolean contains(Format fmt) {
        for (int i = 0; i < rtpFormats.size(); i++) {
            if (rtpFormats.get(i).getFormat().matches(fmt)) {
                return true;
            }
        }
        return false;
    }
    
    public RTPFormat find(Format fmt) {
        for (int i = 0; i < rtpFormats.size(); i++) {
            if (rtpFormats.get(i).getFormat().matches(fmt)) {
                return rtpFormats.get(i);
            }
        }
        return null;
    }
    
    public boolean isEmpty() {
        return rtpFormats.isEmpty();
    }
    
    public void rewind() {
        cursor = 0;
    }
    
    public boolean hasMore() {
        return cursor != rtpFormats.size();
    }
    
    public RTPFormat next() {
        return rtpFormats.get(cursor++);
    }
       
    public boolean hasNonDTMF() {
    	for (int i = 0; i < this.rtpFormats.size(); i++) {
    		if (!this.rtpFormats.get(i).getFormat().getName().equals(AVProfile.telephoneEvent.getName())) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
	public void intersection(RTPFormats other, RTPFormats res) {
		for (int i = 0; i < other.size(); i++) {
			RTPFormat supportedFormat = other.rtpFormats.get(i);
			for (int j = 0; j < this.rtpFormats.size(); j++) {
				RTPFormat offeredFormat = this.rtpFormats.get(j);
				if (supportedFormat.getFormat().matches(offeredFormat.getFormat())) {
					// Add offered (instead of supported) format for DTMF dynamic payload
					res.add(supportedFormat);
					break;
				}
			}
		}
	}
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RTPFormats{");
        
        for (int i = 0; i < rtpFormats.size(); i++) {
            buffer.append(rtpFormats.get(i));
            if (i != rtpFormats.size() -1) buffer.append(",");
        }
        
        buffer.append("}");                
        return buffer.toString();
    }
}
