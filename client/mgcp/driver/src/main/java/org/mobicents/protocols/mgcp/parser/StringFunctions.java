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

package org.mobicents.protocols.mgcp.parser;

import java.util.ArrayList;

import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;

public class StringFunctions 
{
	public static final byte ZERO_BYTE=(byte)'0';
	public static final byte ONE_BYTE=(byte)'1';
	public static final byte TWO_BYTE=(byte)'2';
	public static final byte THREE_BYTE=(byte)'3';
	public static final byte FOUR_BYTE=(byte)'4';
	public static final byte FIVE_BYTE=(byte)'5';
	public static final byte SIX_BYTE=(byte)'6';
	public static final byte SEVEN_BYTE=(byte)'7';
	public static final byte EIGHT_BYTE=(byte)'8';
	public static final byte NINE_BYTE=(byte)'9';
	
	public static final byte EQUAL_BYTE=(byte)'=';
	public static final byte MINUS_BYTE=(byte)'-';
	public static final byte COLON_BYTE=(byte)':';
	public static final byte SEMICOLON_BYTE=(byte)';';
	public static final byte COMMA_BYTE=(byte)',';
	public static final byte AT_BYTE=(byte)'@';
	public static final byte SPACE_BYTE=(byte)' ';
	public static final byte ASTERISK_BYTE=(byte)'*';
	public static final byte DOLLAR_BYTE=(byte)'$';
	public static final byte SLASH_BYTE=(byte)'/';
	public static final byte OPEN_BRACKET_BYTE=(byte)'(';
	public static final byte CLOSE_BRACKET_BYTE=(byte)')';
	public static final byte TAB_BYTE=(byte)'\t';
	public static final byte NEWLINE_BYTE=(byte)'\n';
	public static final byte RETURN_BYTE=(byte)'\r';
	public static final byte DOT_BYTE=(byte)'.';
	
	public static final byte LOW_A_BYTE=(byte)'a';
	public static final byte HIGH_A_BYTE=(byte)'A';
	public static final byte LOW_B_BYTE=(byte)'b';
	public static final byte HIGH_B_BYTE=(byte)'B';
	public static final byte LOW_C_BYTE=(byte)'c';
	public static final byte HIGH_C_BYTE=(byte)'C';
	public static final byte LOW_D_BYTE=(byte)'d';
	public static final byte HIGH_D_BYTE=(byte)'D';
	public static final byte LOW_E_BYTE=(byte)'e';
	public static final byte HIGH_E_BYTE=(byte)'E';
	public static final byte LOW_F_BYTE=(byte)'f';
	public static final byte HIGH_F_BYTE=(byte)'F';
	public static final byte LOW_G_BYTE=(byte)'g';
	public static final byte HIGH_G_BYTE=(byte)'G';
	public static final byte LOW_H_BYTE=(byte)'h';
	public static final byte HIGH_H_BYTE=(byte)'H';
	public static final byte LOW_I_BYTE=(byte)'i';
	public static final byte HIGH_I_BYTE=(byte)'I';
	public static final byte LOW_J_BYTE=(byte)'j';
	public static final byte HIGH_J_BYTE=(byte)'J';
	public static final byte LOW_K_BYTE=(byte)'k';
	public static final byte HIGH_K_BYTE=(byte)'K';
	public static final byte LOW_L_BYTE=(byte)'l';
	public static final byte HIGH_L_BYTE=(byte)'L';
	public static final byte LOW_M_BYTE=(byte)'m';
	public static final byte HIGH_M_BYTE=(byte)'M';
	public static final byte LOW_N_BYTE=(byte)'n';
	public static final byte HIGH_N_BYTE=(byte)'N';
	public static final byte LOW_O_BYTE=(byte)'o';
	public static final byte HIGH_O_BYTE=(byte)'O';
	public static final byte LOW_P_BYTE=(byte)'p';
	public static final byte HIGH_P_BYTE=(byte)'P';
	public static final byte LOW_Q_BYTE=(byte)'q';
	public static final byte HIGH_Q_BYTE=(byte)'Q';
	public static final byte LOW_R_BYTE=(byte)'r';
	public static final byte HIGH_R_BYTE=(byte)'R';
	public static final byte LOW_S_BYTE=(byte)'s';
	public static final byte HIGH_S_BYTE=(byte)'S';
	public static final byte LOW_T_BYTE=(byte)'t';
	public static final byte HIGH_T_BYTE=(byte)'T';
	public static final byte LOW_U_BYTE=(byte)'u';
	public static final byte HIGH_U_BYTE=(byte)'U';
	public static final byte LOW_V_BYTE=(byte)'v';
	public static final byte HIGH_V_BYTE=(byte)'V';
	public static final byte LOW_W_BYTE=(byte)'w';
	public static final byte HIGH_W_BYTE=(byte)'W';
	public static final byte LOW_X_BYTE=(byte)'x';
	public static final byte HIGH_X_BYTE=(byte)'X';
	public static final byte LOW_Y_BYTE=(byte)'y';
	public static final byte HIGH_Y_BYTE=(byte)'Y';
	public static final byte LOW_Z_BYTE=(byte)'z';
	public static final byte HIGH_Z_BYTE=(byte)'Z';
	
	public static final byte CASE_STEP=HIGH_A_BYTE-LOW_A_BYTE;
	
	private static final ConcurrentCyclicFIFO<byte[]> intParsers=new ConcurrentCyclicFIFO<byte[]>();
	private static final ConcurrentCyclicFIFO<ArrayList<SplitDetails>> splitParsers=new ConcurrentCyclicFIFO<ArrayList<SplitDetails>>();
	
	public static int encodeInt(byte[] destination,int offset,int data)
	{
		byte[] tempArray=intParsers.poll();
		if(tempArray==null)
			tempArray=new byte[11];
		
		int currIndex=11;
		if(data==0)
			tempArray[--currIndex]=StringFunctions.ZERO_BYTE;
		else
		{
			while(data>0)
			{
				tempArray[--currIndex]=(byte)(data%10 +ZERO_BYTE);
				data=data/10;
			}
		}
		
		System.arraycopy(tempArray, currIndex, destination, offset, 11-currIndex);
		intParsers.offer(tempArray);
		return 11-currIndex;
	}
	
	public static SplitDetails[] split(byte[] value,int offset,int length,byte splitValue)
	{
		ArrayList<SplitDetails> result=splitParsers.poll();
		if(result==null)
			result=new ArrayList<SplitDetails>();
		
		int startIndex=offset;
		int i=0;
		for(;i<length;i++)
		{
			if(value[i+offset]==splitValue)
			{
				result.add(new SplitDetails(startIndex,i+offset-startIndex));
				startIndex=i+offset+1;
			}
		}
		
		result.add(new SplitDetails(startIndex,i+offset-startIndex));
		SplitDetails[] returnValue=new SplitDetails[result.size()];		
		returnValue=result.toArray(returnValue);
		result.clear();
		splitParsers.offer(result);
		return returnValue;
	}	
	
	public static ArrayList<SplitDetails[]> splitLinesWithTrim(byte[] value,int offset,int length)
    {
		ArrayList<SplitDetails[]> result=new ArrayList<SplitDetails[]>();
		
		ArrayList<SplitDetails> messageResult=splitParsers.poll();
		if(messageResult==null)
			messageResult=new ArrayList<SplitDetails>();
			
		int startIndex=offset;
		int endIndex=startIndex;
		int i=0;
		for(;i<length;i++)
		{
			switch(value[i+offset])
    		{
    			case SPACE_BYTE:
    			case TAB_BYTE:
    				break;
    			case NEWLINE_BYTE:
    				if(endIndex-startIndex==0 && value[startIndex]==DOT_BYTE)
    				{    					
    					SplitDetails[] currMessage=new SplitDetails[messageResult.size()];
    					currMessage=messageResult.toArray(currMessage);
    					result.add(currMessage);
    					messageResult.clear();
    				}
    				else if(endIndex-startIndex>0)
    					messageResult.add(new SplitDetails(startIndex,endIndex-startIndex+1));
    				else
    					messageResult.add(new SplitDetails(startIndex,0));    					
    				
    				startIndex=i+offset+1;
    				endIndex=startIndex;
    				break;
    			case RETURN_BYTE:
    				if(endIndex-startIndex==0 && value[startIndex]==DOT_BYTE)
    				{    					
    					SplitDetails[] currMessage=new SplitDetails[messageResult.size()];
    					currMessage=messageResult.toArray(currMessage);
    					result.add(currMessage);
    					messageResult.clear();
    				}
    				else if(endIndex-startIndex>0)
    					messageResult.add(new SplitDetails(startIndex,endIndex-startIndex+1));
    				else
    					messageResult.add(new SplitDetails(startIndex,0));
    				
    				if(length>i+1 && value[i+offset+1]==NEWLINE_BYTE)
    				{
    					startIndex=i+offset+2;
    					i++;
    				}
    				else
        				startIndex=i+offset+1;
        			
        			endIndex=startIndex;
        			break;
        		default:
        			endIndex=i+offset;
        			break;
    		}
		}
			
		if(endIndex-startIndex>0)
				messageResult.add(new SplitDetails(startIndex,endIndex-startIndex+1));
		
		SplitDetails[] currMessage=new SplitDetails[messageResult.size()];
		currMessage=messageResult.toArray(currMessage);
		result.add(currMessage);
		messageResult.clear();
		splitParsers.offer(messageResult);
		
		return result;		    	 	   
    }
}