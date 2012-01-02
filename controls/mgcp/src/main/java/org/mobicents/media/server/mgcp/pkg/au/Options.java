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

package org.mobicents.media.server.mgcp.pkg.au;

import java.util.Collection;
import org.mobicents.media.server.utils.Text;

/**
 * Represents parameters supplied with command.
 * 
 * @author kulikov
 */
public class Options {
    private final static Text ann = new Text("an");
    private final static Text du = new Text("du");
    private final static Text of = new Text("of");
    private final static Text it = new Text("it");
    private final static Text ip = new Text("ip");
    private final static Text iv = new Text("iv");
    private final static Text mn = new Text("mn");
    private final static Text mx = new Text("mx");
    private final static Text dp = new Text("dp");
    private final static Text ni = new Text("ni");
    private final static Text ri = new Text("ri");
    private final static Text rlt = new Text("rlt");
    private final static Text oa = new Text("oa");
    private final static Text pst = new Text("pst");
    private final static Text cb = new Text("cb");
    private final static Text fdt= new Text("fdt");
    private final static Text idt= new Text("idt");    
    private final static Text na= new Text("na");
    private final static Text eik = new Text("eik");
    private final static Text iek = new Text("iek");
    private final static Text x_md= new Text("x-md");
    
    private final static Text TRUE = new Text("true");
    private final static Text FALSE = new Text("false");
    
    private Text prompt = new Text(new byte[150], 0, 150);
    private Text recordID = new Text(new byte[150], 0, 150);
    
    private boolean isPrompt;
    private boolean override = true;
    
    private Collection<Text> segments;
    
    private int cursor;
    
    //max duration in milliseconds
    private int duration = -1;
    
    //intial offset in milliseconds
    private int offset = 0;
    
    //repeat count
    private int repeatCount;
    
    private int interval;
    
    private int digitsNumber,maxDigitsNumber;
    private long postSpeechTimer = -1;
    
    private Text digitPattern = new Text(new byte[150], 0, 150);
    private Collection digitPatterns;
    
    private Text name = new Text();
    private Text value = new Text();
    
    private Text[] parameter = new Text[]{name, value};
    
    private boolean nonInterruptable = false;
    private long recordDuration = -1;
    private boolean clearDigits = false;
    private boolean includeEndInput = false;
    
    private char endInputKey='#';
    
    private long firstDigitTimer=0;
    private long interDigitTimer=0;
    private long maxDuration=0;
    private int numberOfAttempts=0;
    
    /**
     * Creates options.
     * 
     * @param options the text representation of options.
     */
    public Options(Text options) {
        if (options == null || options.length() == 0) {
            return;
        }
        
        Collection<Text> params = options.split(' ');
        
        for (Text param : params) {
            param.trim();            
            param.divide('=', parameter);
            
            
            if (name.equals(ann)) {
                this.segments = value.split(';');
            } else if (name.equals(du)) {
                this.duration = value.toInteger();
            } else if (name.equals(of)) {
                this.offset = value.toInteger();
            } else if (name.equals(it)) {
                this.repeatCount = value.toInteger();
            } else if (name.equals(iv)) {
                this.interval = value.toInteger();
            } else if (name.equals(ip)) {
                value.duplicate(prompt);
                this.isPrompt = true;
            } else if (name.equals(mn)) {
                this.digitsNumber = value.toInteger();
            } else if (name.equals(mx)) {
                this.maxDigitsNumber = value.toInteger();
            }            
            else if (name.equals(dp)) {                
                value.duplicate(digitPattern);
                digitPatterns = digitPattern.split('|');
            } else if (name.equals(ni)) {
                this.nonInterruptable = value.equals(TRUE);
            } else if (name.equals(ri)) {
                value.duplicate(recordID);
            } else if (name.equals(rlt)) {
                this.recordDuration = value.toInteger() * 1000000L;
            } else if (name.equals(oa)) {
                this.override = value.equals(TRUE);
            } else if (name.equals(pst)) {
                this.postSpeechTimer = value.toInteger() * 100000000L;
            } else if (name.equals(fdt)) {
                this.firstDigitTimer = value.toInteger() * 100000000L;
            } else if (name.equals(idt)) {
                this.interDigitTimer = value.toInteger() * 100000000L;
            } else if (name.equals(x_md)) {
                this.maxDuration = value.toInteger() * 100000000L;
            } else if (name.equals(na)) {
                this.numberOfAttempts = value.toInteger();
            } else if (name.equals(cb)) {
                this.clearDigits = value.equals(TRUE);
            } else if (name.equals(eik) && value.length()==1) {
                this.endInputKey = value.charAt(0);
            } else if (name.equals(iek)) {
                this.includeEndInput = value.equals(TRUE);
            }
        }
    }

    public Collection<Text> getSegments() {
        return segments;
    }
    
    public boolean hasPrompt() {
        return this.isPrompt;
    }
    
    public Text getPrompt() {
        return prompt;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public int getDigitsNumber() {
        return this.digitsNumber;
    }
    
    public int getMaxDigitsNumber() {
        return this.maxDigitsNumber;
    }
    
    public Collection<Text> getDigitPattern() {
        return digitPatterns;
    }
    
    public boolean isNonInterruptable() {
        return this.nonInterruptable;
    }
    
    public Text getRecordID() {
        return this.recordID;
    }
    
    public long getRecordDuration() {
        return this.recordDuration;
    }
    
    public boolean isOverride() {
        return this.override;
    }
    
    public long getPostSpeechTimer() {
        return this.postSpeechTimer;
    }
    
    public long getFirstDigitTimer() {
        return this.firstDigitTimer;
    }
    
    public long getInterDigitTimer() {
        return this.interDigitTimer;
    }
    
    public long getMaxDuration() {
        return this.maxDuration;
    }
    
    public char getEndInputKey() {
        return this.endInputKey;
    }
    
    public int getNumberOfAttempts() {
        return this.numberOfAttempts;
    }    
    
    public boolean isClearDigits() {
        return clearDigits;
    }
    
    public boolean isIncludeEndInputKey() {
        return includeEndInput;
    }
}
