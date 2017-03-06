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

package org.restcomm.media.client.mgcp.parser.params;

import java.text.ParseException;

import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.message.parms.ReturnCode;

public class ReturnCodeHandler
{
	public static ReturnCode decode(byte[] value,int offset,int length) throws ParseException
	{		
		int currIndex=offset;
		int numericValue=0;
		for(int i=0;i<length;i++,currIndex++)
		{			
			if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
				numericValue=numericValue*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
			else 
				throw new ParseException("Invalid return code:" + new String(value,offset,length), 0);
		}
		
		switch(numericValue)
		{
			case ReturnCode.CAS_SIGNALING_PROTOCOL_ERROR:
				return ReturnCode.CAS_Signaling_Protocol_Error;
			case ReturnCode.CONNECTION_WAS_DELETED:
				return ReturnCode.Connection_Was_Deleted;
			case ReturnCode.ENDPOINT_HAS_NO_DIGIT_MAP:
				return ReturnCode.Endpoint_Has_No_Digit_Map;
			case ReturnCode.ENDPOINT_INSUFFICIENT_RESOURCES:
				return ReturnCode.Endpoint_Insufficient_Resources;
			case ReturnCode.ENDPOINT_IS_RESTARTING:
				return ReturnCode.Endpoint_Is_Restarting;
			case ReturnCode.ENDPOINT_NOT_READY:
				return ReturnCode.Endpoint_Not_Ready;
			case ReturnCode.ENDPOINT_REDIRECTED:
				return ReturnCode.Endpoint_Redirected;
			case ReturnCode.ENDPOINT_UNKNOWN:
				return ReturnCode.Endpoint_Unknown;
			case ReturnCode.GATEWAY_CANNOT_DETECT_REQUESTED_EVENT:
				return ReturnCode.Gateway_Cannot_Detect_Requested_Event;
			case ReturnCode.GATEWAY_CANNOT_GENERATE_REQUESTED_SIGNAL:
				return ReturnCode.Gateway_Cannot_Generate_Requested_Signal;
			case ReturnCode.GATEWAY_CANNOT_SEND_SPECIFIED_ANNOUNCEMENT:
				return ReturnCode.Gateway_Cannot_Send_Specified_Announcement;
			case ReturnCode.INCOMPATIBLE_PROTOCOL_VERSION:
				return ReturnCode.Incompatible_Protocol_Version;
			case ReturnCode.INCORRECT_CONNECTION_ID:
				return ReturnCode.Incorrect_Connection_ID;
			case ReturnCode.INSUFFICIENT_BANDWIDTH:
				return ReturnCode.Insufficient_Bandwidth;
			case ReturnCode.INSUFFICIENT_BANDWIDTH_NOW:
				return ReturnCode.Insufficient_Bandwidth_Now;
			case ReturnCode.INSUFFICIENT_RESOURCES_NOW:
				return ReturnCode.Insufficient_Resources_Now;
			case ReturnCode.INTERNAL_HARDWARE_FAILURE:
				return ReturnCode.Internal_Hardware_Failure;
			case ReturnCode.INTERNAL_INCONSISTENCY_IN_LOCALCONNECTIONOPTIONS:
				return ReturnCode.Internal_Inconsistency_In_LocalConnectionOptions;
			case ReturnCode.MISSING_REMOTECONNECTIONDESCRIPTOR:
				return ReturnCode.Missing_RemoteConnectionDescriptor;
			case ReturnCode.NO_SUCH_EVENT_OR_SIGNAL:
				return ReturnCode.No_Such_Event_Or_Signal;
			case ReturnCode.PHONE_OFF_HOOK:
				return ReturnCode.Phone_Off_Hook;
			case ReturnCode.PHONE_ON_HOOK:
				return ReturnCode.Phone_On_Hook;
			case ReturnCode.PROTOCOL_ERROR:
				return ReturnCode.Protocol_Error;
			case ReturnCode.TRANSACTION_BEING_EXECUTED:
				return ReturnCode.Transaction_Being_Executed;
			case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:
				return ReturnCode.Transaction_Executed_Normally;
			case ReturnCode.TRANSIENT_ERROR:
				return ReturnCode.Transient_Error;
			case ReturnCode.TRUNK_GROUP_FAILURE:
				return ReturnCode.Trunk_Group_Failure;
			case ReturnCode.UNKNOWN_CALL_ID:
				return ReturnCode.Unknown_Call_ID;
			case ReturnCode.UNKNOWN_EXTENSION_IN_LOCALCONNECTIONOPTIONS:
				return ReturnCode.Unknown_Extension_In_LocalConnectionOptions;
			case ReturnCode.UNKNOWN_OR_ILLEGAL_COMBINATION_OF_ACTIONS:
				return ReturnCode.Unknown_Or_Illegal_Combination_Of_Actions;
			case ReturnCode.UNRECOGNIZED_EXTENSION:
				return ReturnCode.Unrecognized_Extension;
			case ReturnCode.UNSUPPORTED_OR_INVALID_MODE:
				return ReturnCode.Unsupported_Or_Invalid_Mode;
			case ReturnCode.UNSUPPORTED_OR_UNKNOWN_PACKAGE:
				return ReturnCode.Unsupported_Or_Unknown_Package;
			default:
				// TODO: 0xx should be treated as response acknowledgement.
				if ((numericValue > 99) && (numericValue < 200))
					return (ReturnCode.Transaction_Being_Executed);
				else if ((numericValue > 199) && numericValue < 300)
					return (ReturnCode.Transaction_Executed_Normally);
				else if ((numericValue > 299) && numericValue < 400)
					return (ReturnCode.Endpoint_Redirected);
				else if ((numericValue > 399) && (numericValue < 500))
					return (ReturnCode.Transient_Error);
				else if ((numericValue > 499) && (numericValue < 1000))
					return (ReturnCode.Protocol_Error);
				else
					throw new ParseException("unknown response code: " + numericValue, 0);
		}												
	}
	
	public static int encode(byte[] destination,int offset,ReturnCode returnCode)
	{		
		return StringFunctions.encodeInt(destination,offset,returnCode.getValue());
	}	
}