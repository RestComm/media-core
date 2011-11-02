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
package org.mobicents.javax.media.mscontrol.mediagroup.signals;

/**
 * Reprsents parameters supplied with command.
 * 
 * @author kulikov
 */
public class Options {
    
    private String prompt;
    private String[] segments;
    
    private int cursor;
    
    //max duration in milliseconds
    private int duration = -1;
    
    //intial offset in milliseconds
    private int offset = 0;
    
    //repeat count
    private int repeatCount;
    
    private int interval;
    
    private int mn;
    private String[] dp;
    
    private String dc;
    private int rc;
    private int pi = -1;
    private int fdt=0;
    private int idt=0;
    
    private boolean ni = false;
    private long recordDuration = -1;
    
    private String recordID;
    private boolean isOverride = true;
    
    private boolean silenceTermination = false;
    private long postSpeechTimer;    
    private boolean clearDigits;
    
    /**
     * Creates options.
     * 
     * @param options the text representation of options.
     */
    public Options(String options) {
        if (options == null || options.length() == 0) {
            return;
        }
        
        String[] params = options.split(" ");
        for (int i = 0; i < params.length; i++) {
            String[] tokens = params[i].trim().split("=");
            
            String param = tokens[0];
            String val = tokens[1];
            
            if (param.equalsIgnoreCase("an")) {
                this.segments = parseSegments(val);
            } else if (param.equalsIgnoreCase("du")) {
                this.duration = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("of")) {
                this.offset = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("it")) {
                this.repeatCount = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("iv")) {
                this.interval = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("ip")) {
                this.prompt = val;
            } else if (param.equalsIgnoreCase("mn")) {
                this.mn = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("dp")) {
                this.dp = val.split("|");
            } else if (param.equalsIgnoreCase("dc")) {
                this.dc = val;
            } else if (param.equalsIgnoreCase("rc")) {
                this.rc = Integer.parseInt(val);
            } else if (param.equalsIgnoreCase("pi")) {
                this.pi = Integer.parseInt(val);
            } else if(param.equalsIgnoreCase("fdt")) {
            	this.fdt=Integer.parseInt(val);
            } else if(param.equalsIgnoreCase("idt")) {
            	this.idt=Integer.parseInt(val);
            }
        }
    }
    
    public Options() {
    }
    
    public boolean hasMoreSegments() {
        return cursor < segments.length;
    }
    
    public String next() {
        return segments[cursor++];
    }
    
    private String[] parseSegments(String s) {
        //cut brakects
        if (s.startsWith("(")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.split(";");        
    }
    
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
        return mn;
    }
    
    public void setFirstDigitTimer(int fdt) {
        this.fdt=fdt;
    }
    
    public int getFirstDigitTimer() {
        return fdt;
    }
    
    public void setInterDigitTimer(int idt) {
        this.idt=idt;
    }
    
    public int getInterDigitTimer() {
        return idt;
    }
    
    public void setDigitsNumber(int mn) {
        this.mn = mn;
    }
    
    public String[] getDigitPattern() {
        return dp;
    }
    
    public void setDigitPattern(String[] dp) {
        this.dp = dp;
    }
    
    public String getDigitsCollected() {
        return dc;
    }
    
    public int getReturnCode() {
        return rc;
    }
    
    public int getPatternIndex() {
        return pi;
    }

    public boolean isNonInterruptablePlay() {
        return ni;
    }

    public void setNonInterruptiblePlay(boolean ni) {
        this.ni = ni;
    }

    public void setRecordID(String recordID) {
        this.recordID = recordID;
    }
    
    public String getRecordID() {
        return this.recordID;
    }
    
    public void setRecordDuraion(long recordDuration) {
        this.recordDuration = recordDuration;
    }
    
    public long getRecordDuration() {
        return this.recordDuration;
    }
    
    public void setOverride(boolean isOverride) {
        this.isOverride = isOverride;
    }
    
    public void setSilenceTermination(boolean val) {
        this.silenceTermination = val;
    }
    
    public void setPostSpeechTimer(long postSpeechTimer) {
        this.postSpeechTimer = postSpeechTimer;
    }
    
    public void setClearDigits(boolean val) {
        this.clearDigits = val;
    }
    
    public void processFilters(String[] filters) {
        if (filters == null) {
            return;
        }
        
        for (String filter : filters) {
            this.dc = dc.replaceAll(filter, "");
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        if (mn > 0) {
            buff.append("mn=").append(mn);
        }
        
        if (dp != null) {
            buff.append(" ");
            buff.append("dp=");
            for (int i = 0; i < dp.length - 1; i++) {
                buff.append(dp[i]);
                if (i <= dp.length - 2) {
                    buff.append("|");
                }
            }
            buff.append(dp[dp.length - 1]);
        }

        if (prompt != null) {
            buff.append(" ");
            buff.append("ip=");
            buff.append(prompt);
        }

        if (ni) {
            buff.append(" ");
            buff.append("ni=true");
        }
        
        if (this.recordDuration > 0) {
            buff.append( " ");
            buff.append("rlt=").append(recordDuration);
        }
        
        if (this.recordID != null) {
            buff.append( " ");
            buff.append("ri=").append(recordID);
        }
        
        if (!this.isOverride) {
            buff.append(" ");
            buff.append("oa=false");
        }

        if (this.silenceTermination) {
            buff.append(" ");
            buff.append("pst=40");
        }

        if (this.postSpeechTimer > 0) {
            buff.append(" ");
            buff.append("pst=").append(postSpeechTimer/100);
        }
        
        if (this.fdt > 0) {
            buff.append(" ");
            buff.append("fdt=").append(fdt/100);
        }
        
        if (this.idt > 0) {
            buff.append(" ");
            buff.append("idt=").append(idt/100);
        }
        
        if (this.clearDigits) {
            buff.append(" ");
            buff.append("cb=true");
        }
        
        return buff.toString().trim();
    }
}
