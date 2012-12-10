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
 * File Name     : MgcpMessageParser.java
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

package org.mobicents.protocols.mgcp.parser;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;

import java.text.ParseException;
import org.apache.log4j.Logger;


/**
 * Provides processing of the MGCP message.
 *
 * @author Yulian Oifa
 */
public class MgcpMessageParser {
  
    private MgcpContentHandler contentHandler;
    private static final Logger logger = Logger.getLogger(MgcpMessageParser.class);
    private long totalTime=0;
    /** Creates a new instance of MgcpMessageParser */
    public MgcpMessageParser(MgcpContentHandler contentHandler) {
        if (contentHandler == null) {
            throw new IllegalArgumentException("Content handler cannot be null");
        }
        this.contentHandler = contentHandler;
    }
    
    public void parse(String message) throws IOException, ParseException {
    	int currIndex=0;
    	ParamLineValue paramOutput=new ParamLineValue();
    	LineValue output=new LineValue();
    	readLineWithTrim(message,currIndex,output);
    	String header=output.output;
    	currIndex=output.newIndex;
    	contentHandler.header(header);

        boolean sdpPresent = false;               
    	String line=null;
    	while (currIndex<message.length()) {
        	readParamLineWithTrim(message,currIndex,paramOutput);
        	currIndex=paramOutput.newIndex;
        	
        	sdpPresent = (paramOutput.name==null || paramOutput.name.length()==0);
            if (sdpPresent) break;
            
            if (paramOutput.value==null) {
                logger.warn("Unrecognized parameter: " + line);
                continue;
            }
                        
            contentHandler.param(paramOutput.name, paramOutput.value);                       
        }
        
        if(sdpPresent)
        {
        	long startTime=System.nanoTime();
        	StringBuilder sdp=new StringBuilder();
        	while (currIndex<message.length()) {
        		readLineWithTrim(message,currIndex,output);
            	line=output.output;
            	currIndex=output.newIndex;
            	sdp.append(line);
        	}
        	
        	if (logger.isDebugEnabled()) {
                logger.debug("Read session description: " + sdp.toString());
            }
        	
        	contentHandler.sessionDescription(sdp.toString());                
        }
    }
    
    private void readParamLineWithTrim(String source,int startIndex,ParamLineValue result)
    {
    	result.name=null;
    	result.value=null;
    	char currChar;
    	int i=startIndex;    	
    	for(;i<source.length();i++)
    	{
    		currChar=source.charAt(i);
    		if(currChar!=' ' && currChar!='\t')
    			break;
    		
    		startIndex++;
    	}
    	
    	int endIndex=i;
    	for(;i<source.length();i++)
    	{    		
    		currChar=source.charAt(i);
    		if(currChar==':')
    		{
    			i++;
    	    	break;	
    		}
    		
    		switch(currChar)
    		{
    			case ' ':
    			case '\t':
    				break;
    			case '\n':
    				result.name=source.substring(startIndex,endIndex);
        			result.newIndex=i+1;
        			return;
    			case '\r':
    				result.name=source.substring(startIndex,endIndex);
        			if(source.length()>i+1 && source.charAt(i+1)=='\n')
        				result.newIndex=i+1;    				
        			else 
        				result.newIndex=i+2;        			
        			return;
    			default:
        			endIndex=i+1;        			        			
        			break;
    		}
    	}
    	
    	if(i==source.length())
    	{
        	result.name=source.substring(startIndex);
        	result.newIndex=source.length();
        	return;
    	}
    	else
        	result.name=source.substring(startIndex,endIndex);

        startIndex=i;  
    	for(;i<source.length();i++)
    	{
    		currChar=source.charAt(i);
    		if(currChar!=' ' && currChar!='\t')
    			break;
    		
    		startIndex++;
    	}
    	
    	endIndex=i;
    	for(;i<source.length();i++)
    	{    		
    		currChar=source.charAt(i);
    		switch(currChar)
    		{
    			case ' ':
    			case '\t':
    				break;
    			case '\n':
    				result.value=source.substring(startIndex,endIndex);
        			result.newIndex=i+1;
        			return;
    			case '\r':
    				result.value=source.substring(startIndex,endIndex);
        			if(source.length()>i+1 && source.charAt(i+1)=='\n')
        				result.newIndex=i+1;    				
        			else 
        				result.newIndex=i+2;        			
        			return;
        		default:
        			endIndex=i+1;
        			break;
    		}
    	}
    	
    	if(result.name==null)
    	{
    		if(endIndex==source.length())
				result.name=source.substring(startIndex);
			else
				result.name=source.substring(startIndex,endIndex);
    	}
    	else
    	{
    		if(endIndex==source.length())
    			result.value=source.substring(startIndex);
    		else
    			result.value=source.substring(startIndex,endIndex);
    	}
    	
    	result.newIndex=source.length();    	
    }
    
    private void readLineWithTrim(String source,int startIndex,LineValue result)
    {
    	char currChar;
    	int i=startIndex;    	
    	for(;i<source.length();i++)
    	{
    		currChar=source.charAt(i);
    		if(currChar!=' ' && currChar!='\t')
    			break;
    		
    		startIndex++;
    	}
    	
    	int endIndex=i;
    	for(;i<source.length();i++)
    	{    		
    		currChar=source.charAt(i);
    		switch(currChar)
    		{
    			case ':':
    				
    				break;
    			case ' ':
    			case '\t':
    				break;
    			case '\n':
    				result.output=source.substring(startIndex,endIndex);
        			result.newIndex=i+1;
        			return;
    			case '\r':
    				result.output=source.substring(startIndex,endIndex);
        			if(source.length()>i+1 && source.charAt(i+1)=='\n')
        				result.newIndex=i+1;    				
        			else 
        				result.newIndex=i+2;
        			
        			return;
        		default:
        			endIndex=i+1;
        			break;
    		}    		
    	}
    	
    	if(endIndex==source.length())
    		result.output=source.substring(startIndex);
    	else
    		result.output=source.substring(startIndex,endIndex);
    	
    	result.newIndex=source.length();    	    
    }
    
    private class ParamLineValue
    {
    	protected int newIndex;    	
    	protected String name;
    	protected String value;
    }
    
    private class LineValue
    {
    	protected int newIndex;
    	protected String output;
    }
}
