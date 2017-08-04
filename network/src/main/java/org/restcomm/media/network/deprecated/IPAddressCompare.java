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
package org.restcomm.media.network.deprecated;

import sun.net.util.IPAddressUtil;

/**
 * Helper functions to check whether ip address is in network with specified subnet mask
 * used in proxy leg implementation
 * 
 * @author Yulian Oifa
 */
public class IPAddressCompare 
{

	/**
	*  Checks whether ipAddress is in IPV4 network with specified subnet 
	*/
    public static boolean isInRangeV4(byte[] network,byte[] subnet,byte[] ipAddress)
    {
    	if(network.length!=4 || subnet.length!=4 || ipAddress.length!=4)
    		return false;    	
    	
    	return compareByteValues(network,subnet,ipAddress);
    }
    
    /**
	*  Checks whether ipAddress is in IPV6 network with specified subnet 
	*/
    public static boolean isInRangeV6(byte[] network,byte[] subnet,byte[] ipAddress)
    {
    	if(network.length!=16 || subnet.length!=16 || ipAddress.length!=16)
    		return false;
    	
    	return compareByteValues(network,subnet,ipAddress);
    }
    
    /**
	*  Checks whether ipAddress is in network with specified subnet by comparing byte logical end values 
	*/
    private static boolean compareByteValues(byte[] network,byte[] subnet,byte[] ipAddress)
    {    	
    	for(int i=0;i<network.length;i++)
    		if((network[i] & subnet[i]) != (ipAddress[i] & subnet[i]))
    			return false;
    	
    	return true;
    }

    /**
	*  Gets IP address type 
	*/
    public static IPAddressType getAddressType(String ipAddress)
    {
    	if(IPAddressUtil.isIPv4LiteralAddress(ipAddress))
    		return IPAddressType.IPV4;
    	
    	if(IPAddressUtil.isIPv6LiteralAddress(ipAddress))
    		return IPAddressType.IPV6;
    	
    	return IPAddressType.INVALID;
    }
    
    /**
	*  Converts String to byte array for IPV4 , returns null if ip address is not legal  
	*/
    public static byte[] addressToByteArrayV4(String ipAddress)
    {
    	return IPAddressUtil.textToNumericFormatV4(ipAddress);
    }
    
    /**
	*  Converts String to byte array for IPV6 , returns null if ip address is not legal 
	*/
    public static byte[] addressToByteArrayV6(String ipAddress)
    {
    	return IPAddressUtil.textToNumericFormatV6(ipAddress);
    }
}
