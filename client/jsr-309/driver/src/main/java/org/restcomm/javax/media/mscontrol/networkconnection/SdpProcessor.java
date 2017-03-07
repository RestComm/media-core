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
package org.restcomm.javax.media.mscontrol.networkconnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

/**
 *
 * @author kulikov
 */
public class SdpProcessor {
    
    
    private HashMap<String, RTPFormat> profile = new HashMap();
    
    public SdpProcessor() {
        profile.put("0", new RTPFormat("0", "pcmu/8000"));
        profile.put("8", new RTPFormat("8", "pcma/8000"));
    }
    
    public boolean containsFormat(String format, SessionDescription sdp) throws SdpException {
        Vector<MediaDescription> mds = sdp.getMediaDescriptions(false);
        for (MediaDescription m : mds) {
            Vector<Attribute> attributes = m.getAttributes(false);
            for (Attribute a : attributes) {
                if (a.getName().equalsIgnoreCase("rtpmap") && a.getValue().toLowerCase().contains(format.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsMedia(String media, SessionDescription sdp) throws SdpException {
        Vector<MediaDescription> mds = sdp.getMediaDescriptions(false);
        for (MediaDescription m : mds) {
            if (m.getMedia().getMediaType().equalsIgnoreCase(media)) {
                return true;
            }
        }
        return false;
    }
    
    public void exclude(String f, SessionDescription sdp) throws SdpException {
        Vector<MediaDescription> mds = sdp.getMediaDescriptions(false);
        for (MediaDescription m : mds) {
            Collection<RTPFormat> fmts = getRTPFormats(m);
            for (RTPFormat rtpFormat : fmts) {
                if (rtpFormat.description.toLowerCase().contains(f.toLowerCase())) {
                    this.excludeFormat(rtpFormat.id, m);
                    this.excludeAttribute(rtpFormat.id, m);
                }
            }
        }
    }
    
    private void excludeFormat(String id, MediaDescription m) throws SdpParseException {
        Vector<String> fmts = m.getMedia().getMediaFormats(false);
        
        int count = 0;
        for (String s : fmts) {
            if (s.equals(id)) {
                break;
            }
            count++;
        }

        if (count < fmts.size()) {
            fmts.remove(count);
        }
    }

    public void excludeAttribute(String id, MediaDescription m) throws SdpParseException {
        Vector<Attribute> attributes = m.getAttributes(false);
        int count = 0;
        
        for (Attribute a : attributes) {
            if (a.getName().equalsIgnoreCase("rtpmap")) {
                String[] tokens = a.getValue().split(" ");

                if (tokens[0].trim().equalsIgnoreCase(id)) {
                    break;
                }

            }
            count++;
        }
        
        if (count < attributes.size()) { 
            attributes.remove(count);
        }
    }
    
    public boolean checkForMinimalOffer(SessionDescription sdp) throws SdpException {
        Vector<MediaDescription> mds = sdp.getMediaDescriptions(false);
        for (MediaDescription m : mds) {
            if (m.getMedia().getMediaFormats(false) == null) {
                return false;
            }
        }
        return true;
    }
    
    private Collection<RTPFormat> getRTPFormats(MediaDescription md) throws SdpParseException {
        ArrayList<RTPFormat> formats = new ArrayList();
        
        Vector<String> fmts = md.getMedia().getMediaFormats(false);
        Vector<Attribute> attributes = md.getAttributes(false);
        
        for (String f : fmts) {
            RTPFormat format = null;
            
            for (Attribute a : attributes) {
                if (a.getName().equalsIgnoreCase("rtpmap")) {
                    String[] tokens = a.getValue().split(" ");
                    
                    if (tokens[0].equalsIgnoreCase(f)) {
                        format = new RTPFormat(tokens[0].trim(), tokens[1].trim());
                        break;
                    }
                    
                }
            }
            
            if (format == null) {
                format = profile.get(f);
            }
            
            if (format != null) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private class RTPFormat {
        private String id;
        private String description;
        
        public RTPFormat(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        @Override
        public String toString() {
            return id + ":" + description;
        }
    }
    
}
