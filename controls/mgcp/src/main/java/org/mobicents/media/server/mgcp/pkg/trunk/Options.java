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

package org.mobicents.media.server.mgcp.pkg.trunk;

import java.util.Collection;
import org.mobicents.media.server.utils.Text;

/**
 * Represents parameters supplied with command.
 * 
 * @author oifa yulian
 */
public class Options {
    private final static Text in = new Text("in");
    private final static Text out = new Text("out");
    private final static Text plus = new Text("+");
    private final static Text minus = new Text("-");    
    
    private Text inTone = new Text();
    private Text outTone = new Text();
    
    private boolean shouldActivate=true;    
    private boolean shouldDeactivate = false;
    
    private Text name = new Text();
    private Text value = new Text();
    
    private Text[] parameter = new Text[]{name, value};
    
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
        int count;
        for (Text param : params) {
            param.trim();            
            count=param.divide('=', parameter);
            
            if(count==2) {
            	switch(name.charAt(0))
                {
                	case 'i':
                	case 'I':
                		if (name.equals(in)) 
                			this.inTone = value;
                		break;
                	case 'o':
                	case 'O':
                		if (name.equals(out))
                			this.outTone = value;	
                		break;                	            		
                }	
            }   
            else if(name.length()==1) {
            	switch(name.charAt(0))
                {
                	case '+':
                		if(name.length()==1)
                		{
                			this.shouldActivate = true;
                            this.shouldDeactivate=false;
                		}
                		break;
                	case '-':
                		if(name.length()==1)
                		{
                			this.shouldActivate = false;
                            this.shouldDeactivate=true;
                		}
                		break;            			
                }	
            }
        }
    }

    public boolean isActivation() {
        return this.shouldActivate;
    }
    
    public boolean isDeactivation() {
        return this.shouldDeactivate;
    }
       
    public Text getInTone() {
        return this.inTone;
    }
    
    public Text getOutTone() {
        return this.outTone;
    }
}
