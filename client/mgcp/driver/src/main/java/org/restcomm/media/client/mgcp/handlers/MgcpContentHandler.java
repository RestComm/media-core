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

/*
 * File Name     : MgcpContentHandler.java
 *
 * The JAIN MGCP API implementaion.
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.restcomm.media.client.mgcp.handlers;

import java.text.ParseException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;

/**
 * Receive notification of the logical content of a message. 
 *
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 * @author Yulian Oifa
 */
public abstract class MgcpContentHandler 
{
	private static final Logger logger = Logger.getLogger(MgcpContentHandler.class);
    
	/**
     * Receive notification of the parameter of a message.
     * Parser will call this method to report about parameter reading.
     *
     * @param name the name of the paremeter
     * @param value the value of the parameter.
     */
    public abstract void param(byte[] data,SplitDetails name, SplitDetails value) throws ParseException;
    
    /**
     * Receive notification of the session description.
     * Parser will call this method to report about session descriptor reading.
     *
     * @param sd the session description from message.
     */
    public abstract void sessionDescription(String sd) throws ParseException;
    
    public void parse(byte[] data,SplitDetails[] message) throws IOException, ParseException 
    {
    	boolean sdpPresent = false;    
    	int i=1;
    	for(;i<message.length;i++)
    	{
    		sdpPresent = (message[i].getLength()==0);
            if (sdpPresent) break;
            
        	SplitDetails[] paramDetails=readParamLineWithTrim(data,message[i]);                       
            if (paramDetails.length==1) 
            {
                logger.warn("Unrecognized parameter: " + new String(data,message[i].getOffset(),message[i].getLength()));
                continue;
            }
                        
            param(data, paramDetails[0], paramDetails[1]);                       
        }
        
        if(sdpPresent && i<message.length)
        {
        	StringBuilder sdp=new StringBuilder();
        	for(;i<message.length-1;i++)
        		sdp.append(new String(data,message[i].getOffset(),message[i].getLength())).append("\r\n");
        	
        	sdp.append(new String(data,message[i].getOffset(),message[i].getLength()));
        	
        	if (logger.isDebugEnabled()) 
        	    logger.debug("Read session description: " + sdp.toString());            
        	
        	sessionDescription(sdp.toString());                
        }
    }
    
    private SplitDetails[] readParamLineWithTrim(byte[] source,SplitDetails lineDetails)
    {
    	byte currByte;
    	int i=0;
    	int startIndex=lineDetails.getOffset();
    	for(;i<lineDetails.getLength();i++)
    	{
    		currByte=source[lineDetails.getOffset() + i];
    		if(currByte!=StringFunctions.SPACE_BYTE && currByte!=StringFunctions.TAB_BYTE)
    			break;
    		
    		startIndex++;
    	}
    	
    	int endIndex=startIndex;
    	Boolean colonFound=false;
    	for(;i<lineDetails.getLength();i++)
    	{    		
    		currByte=source[lineDetails.getOffset() + i];
    		if(currByte==StringFunctions.COLON_BYTE)
    		{
    			colonFound=true;
    			i++;
    	    	break;
    		}
    		else if(currByte!=StringFunctions.SPACE_BYTE && currByte!=StringFunctions.TAB_BYTE)
    			endIndex= lineDetails.getOffset() + i;
    	}
    	
    	if(i==lineDetails.getLength())
    	{
    		if(colonFound)
    		{
    			SplitDetails[] result=new SplitDetails[2];
    			result[0]=new SplitDetails(startIndex,endIndex-startIndex+1);
    			result[1]=new SplitDetails(endIndex+1,0);
    			return result;
    		}
    		else
    		{
    			SplitDetails[] result=new SplitDetails[1];
    			result[0]=new SplitDetails(startIndex,endIndex-startIndex+1);
    			return result;
    		}
    	}
    	
    	SplitDetails[] result=new SplitDetails[2];
		result[0]=new SplitDetails(startIndex,endIndex-startIndex+1);
        
        startIndex=lineDetails.getOffset() + i;  
    	for(;i<lineDetails.getLength();i++)
    	{
    		currByte=source[lineDetails.getOffset() + i];
    		if(currByte!=StringFunctions.SPACE_BYTE && currByte!=StringFunctions.TAB_BYTE)
    			break;
    		
    		startIndex++;    		
    	}
    	
    	endIndex=lineDetails.getOffset()+lineDetails.getLength()-1;
    	for(;i<lineDetails.getLength();i++)
    	{    		
    		currByte=source[endIndex];
    		if(currByte!=StringFunctions.SPACE_BYTE && currByte!=StringFunctions.TAB_BYTE)
    			break;
    		
    		endIndex--;    		    		
    	}
    	
    	result[1]=new SplitDetails(startIndex,endIndex-startIndex+1);
        return result;   	
    }    
}