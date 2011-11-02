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

    private boolean ni = false;

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
            } if (param.equalsIgnoreCase("mn")) {
                this.mn = Integer.parseInt(val);
            } if (param.equalsIgnoreCase("dp")) {                
                this.dp = val.split("\\|");
            } if (param.equalsIgnoreCase("ni")) {
                this.ni = Boolean.parseBoolean(val);
            }
        }
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
    
    public String[] getDigitPattern() {
        return dp;
    }

    public boolean isNonInterruptablePlay() {
        return ni;
    }

    public void setNonInterruptablePlay(boolean ni) {
        this.ni = ni;
    }

}
