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
package org.restcomm.mscontrol.sdp;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public class MediaDescriptor {
    
    protected MediaType mediaType;
    private int port;
    private String profile;
    
    private FormatParser fmtParser;
    
    private int[] fmt = new int[15];
    private Format[] formats = new Format[15];
    
    private int count;
    
    private String[] attributes = new String[15];
    private int aCount;
    
    private String mode;
    
    private Connection connection;
    
    private int length = 0;
    private char[] chars = null;
    
    public MediaDescriptor(String m) {
    	length = m.length();
    	chars = m.toCharArray();
    	
        int pos1 = m.indexOf(61);
        int pos2 = getNextSpace(pos1+1);
        
        mediaType = MediaType.getInstance(m.substring(pos1 + 1, pos2));
        
        if (mediaType == MediaType.AUDIO) {
            fmtParser = new AudioFormatParser();
        } else if (mediaType == MediaType.VIDEO) {
            fmtParser = new VideoFormatParser();
        } else {
            throw new IllegalArgumentException("Unknown media type " + mediaType);
        }
        
        pos1 = getNextSpace(pos2+1);
        port = Integer.parseInt(m.substring(pos2 + 1, pos1));
        
        pos2 = getNextSpace(pos1+1);
        profile = m.substring(pos1 + 1, pos2).trim();
        
        pos1 = pos2;
        pos2 = getNextSpace(pos1+1);
        
        Format format = null;
        while (pos2 > 0) {
            int f = Integer.parseInt(m.substring(pos1 + 1, pos2).trim());
            format = fmtParser.getFormat(f);
            
            if(format!=null){
            	fmt[count] = f;
            	formats[count++] = format;
            }
            
            pos1 = pos2;
            pos2 = getNextSpace(pos1+1);
        }
        
        
//        format = fmtParser.getFormat(fmt[count]);
//        
//        if(format != null){
//        	fmt[count] = Integer.parseInt(m.substring(pos1 + 1, m.length()));
//        	formats[count] = format;
//        	
//            count++;
//        }
        

    }
    
    private int getNextSpace(int from) {
        int next = 0;

        //If from is end of line there is no scope to check for space
        if (from >= length) {
            return 0;
        }

        for (int i = from; i < length - 1; i++) {
            if (chars[i] == 32 && chars[i + 1] != 32) {
                next = i;
                break;
            }
        }

        //If we reach end of line where there is no space we return length
        if (next == 0) {
            return length;
        }

        return next;
    }
    
    public MediaDescriptor(MediaType mediaType, int port) {
        this.mediaType = mediaType;
        this.port = port;
        
        fmtParser = new AudioFormatParser();
        if (mediaType == MediaType.AUDIO) {
            fmtParser = new AudioFormatParser();
        } else if (mediaType == MediaType.VIDEO) {
            fmtParser = new VideoFormatParser();
        } else {
            throw new IllegalArgumentException("Unknown media type " + mediaType);
        }
    }

    
    public MediaType getMediaType() {
        return mediaType;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getProfle() {
        return profile;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public int getFormatCount() {
        return count;
    }
        
    public int getPyaloadType(int i) {
        return fmt[i];
    }
    
    public Format getFormat(int i) {
        return formats[i];
    }
    
    public void addFormat(int payload, Format format) {
        fmt[count] = payload;
        formats[count++] = format;
    }
    
    public void addAttribute(String attribute) {
        attributes[aCount++] = attribute;
    }
    
    public void exclude(Format format) {
        int k = 0;
        
        for (int i = 0; i < count; i++) {
            if (formats[i] != null && formats[i].equals(format)) {
                k = i;
                break;
            }
        }
        
        System.arraycopy(formats, k + 1, formats, k, formats.length - k -1);
        System.arraycopy(fmt, k + 1, fmt, k, fmt.length - k - 1);
        
        count--;
    }

    public void exclude(String formatName) {
        int k = -1;
        
        for (int i = 0; i < count; i++) {
            if (formats[i] != null && formats[i].matches(formatName)) {
                k = i;
                break;
            }
        }
        
        if (k == -1) {
            return;
        }
        
        System.arraycopy(formats, k + 1, formats, k, formats.length - k -1);
        System.arraycopy(fmt, k + 1, fmt, k, fmt.length - k - 1);
        
        count--;
    }
    
    protected boolean contains(String encoding) {
        for (int i = 0; i < count; i++) {
            if (formats[i] != null && formats[i].matches(encoding)) {
                return true;
            }
        }
        return false;
    }
    
    protected void parseAtribute(String a) {
        if (a.startsWith("a=rtpmap:")) {
            if (fmtParser.parse(a, fmt, formats, count)) {
                count++;
            }
        } else if (a.equals("a=sendrecv")) {
            this.mode = "sendrecv";
        } else if (a.equals("a=sendonly")) {
            this.mode = "sendonly";
        } else if (a.equals("a=recvonly")) {
            this.mode = "recvonly";
        } else if (a.startsWith("c=")) {
        	connection = new Connection(a);
        }
    }
    
    public void write(StringBuffer buffer) {
        buffer.append("m=" + mediaType.getName() + " " + port  + " RTP/AVP");
        for (int j = 0; j < count; j++) {
            buffer.append(" " + fmt[j]);
        }        
        buffer.append("\n");
        
        if(this.connection != null ){
        	buffer.append(connection.toString() + "\n");
        }
        
        for (int j = 0; j < aCount; j++) {
            buffer.append("a=" + attributes[j] + "\n");
        }
        
        for (int j = 0; j < count; j++) {
            fmtParser.write(buffer, fmt[j], formats[j]);
            buffer.append("\n");
        }
        
        if(this.mediaType== MediaType.AUDIO) {
        	buffer.append("a=control:audio\n");
        } else if(this.mediaType== MediaType.VIDEO){
        	buffer.append("a=control:video\n");
        }
        
        buffer.append("a=silenceSupp:off\n");
    }

	public Connection getConnection() {
		return connection;
	}
}
