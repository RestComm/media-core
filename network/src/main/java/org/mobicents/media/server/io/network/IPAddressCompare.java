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
package org.mobicents.media.server.io.network;

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
