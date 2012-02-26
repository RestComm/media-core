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

package org.mobicents.media.server.impl.dsp.audio.ilbc;

/**
 * 
 * @author oifa yulian 
 */
public class Constants {
	public static final short HP_IN_COEFICIENTS[] = {(short)3798,(short)-7596,(short)3798,(short)7807,(short)-3733};
	public static final short HP_OUT_COEFICIENTS[] = {(short)3849,(short)-7699,(short)3849,(short)7918,(short)-3833};
	
	public static final int LPC_LAG_WIN[]={2147483647,2144885453,2137754373,2125918626,2109459810,2088483140,2063130336,2033564590,1999977009,1962580174,1921610283};
	
	public static final short LPC_ASYM_WIN[] = {
		(short)2, (short)7, (short)15, (short)27, (short)42, (short)60, (short)81, (short)106, (short)135, (short)166, (short)201, (short)239,
		(short)280, (short)325, (short)373, (short)424, (short)478, (short)536, (short)597, (short)661, (short)728, (short)798, (short)872, (short)949,
		(short)1028, (short)1111, (short)1197, (short)1287, (short)1379, (short)1474, (short)1572, (short)1674, (short)1778, (short)1885, (short)1995, (short)2108,
		(short)2224, (short)2343, (short)2465, (short)2589, (short)2717, (short)2847, (short)2980, (short)3115, (short)3254, (short)3395, (short)3538, (short)3684,
		(short)3833, (short)3984, (short)4138, (short)4295, (short)4453, (short)4615, (short)4778, (short)4944, (short)5112, (short)5283, (short)5456, (short)5631,
		(short)5808, (short)5987, (short)6169, (short)6352, (short)6538, (short)6725, (short)6915, (short)7106, (short)7300, (short)7495, (short)7692, (short)7891,
		(short)8091, (short)8293, (short)8497, (short)8702, (short)8909, (short)9118, (short)9328, (short)9539, (short)9752, (short)9966, (short)10182, (short)10398,
		(short)10616, (short)10835, (short)11055, (short)11277, (short)11499, (short)11722, (short)11947, (short)12172, (short)12398, (short)12625, (short)12852, (short)13080,
		(short)13309, (short)13539, (short)13769, (short)14000, (short)14231, (short)14463, (short)14695, (short)14927, (short)15160, (short)15393, (short)15626, (short)15859,
		(short)16092, (short)16326, (short)16559, (short)16792, (short)17026, (short)17259, (short)17492, (short)17725, (short)17957, (short)18189, (short)18421, (short)18653,
		(short)18884, (short)19114, (short)19344, (short)19573, (short)19802, (short)20030, (short)20257, (short)20483, (short)20709, (short)20934, (short)21157, (short)21380,
		(short)21602, (short)21823, (short)22042, (short)22261, (short)22478, (short)22694, (short)22909, (short)23123, (short)23335, (short)23545, (short)23755, (short)23962,
		(short)24168, (short)24373, (short)24576, (short)24777, (short)24977, (short)25175, (short)25371, (short)25565, (short)25758, (short)25948, (short)26137, (short)26323,
		(short)26508, (short)26690, (short)26871, (short)27049, (short)27225, (short)27399, (short)27571, (short)27740, (short)27907, (short)28072, (short)28234, (short)28394,
		(short)28552, (short)28707, (short)28860, (short)29010, (short)29157, (short)29302, (short)29444, (short)29584, (short)29721, (short)29855, (short)29987, (short)30115,
		(short)30241, (short)30364, (short)30485, (short)30602, (short)30717, (short)30828, (short)30937, (short)31043, (short)31145, (short)31245, (short)31342, (short)31436,
		(short)31526, (short)31614, (short)31699, (short)31780, (short)31858, (short)31933, (short)32005, (short)32074, (short)32140, (short)32202, (short)32261, (short)32317,
		(short)32370, (short)32420, (short)32466, (short)32509, (short)32549, (short)32585, (short)32618, (short)32648, (short)32675, (short)32698, (short)32718, (short)32734,
		(short)32748, (short)32758, (short)32764, (short)32767, (short)32767, (short)32667, (short)32365, (short)31863, (short)31164, (short)30274, (short)29197, (short)27939,
		(short)26510, (short)24917, (short)23170, (short)21281, (short)19261, (short)17121, (short)14876, (short)12540, (short)10126, (short)7650, (short)5126, (short)2571
	};
	
	
	public static short abs(short var1)
    {
    	if(var1==Short.MIN_VALUE)
    		return Short.MAX_VALUE;
    	else if(var1<0)
    		return (short)-var1;
    	
    	return (short)var1;
    }
	
	public static short norm(int var1) throws ArithmeticException
    {
    	if(var1==0)
    		throw new ArithmeticException("Value can not be 0");
    	    	
    	int currValue=var1;    	
    	if(var1<0)
    	{
    		if(var1<=0xC0000000)
    			return 0;
    		    		
    		currValue=~(var1-1);
    	}
    	
    	short count=-1;
    	//check 16 bits sequence
    	if(currValue>>16!=0)
    		currValue=currValue>>16;
    	else
    		count+=16;
    	
    	//check 8 bits sequence
    	if(currValue>>8!=0)
    		currValue=currValue>>8;
    	else
    		count+=8;
    	
    	//check 4 bits sequence
    	if(currValue>>4!=0)
    		currValue=currValue>>4;
    	else
    		count+=4;
    	
    	//check 2 bits sequence
    	if(currValue>>2!=0)
    		currValue=currValue>>2;
    	else
    		count+=2;
    	
    	//check 1 bits sequence
    	if(currValue>>1==0)
    		count+=1;
    	
    	return count;
    }
	
	public static int div(int num, short hi,short low)
	{
	    short temp1, temp2, numHi, numLow;
	    int temp,approx;
	    
	    if(hi!=0)
	    	approx=0x1FFFFFFF/hi;
	    else
	    	approx=Integer.MAX_VALUE;
	    	    
	    temp = (hi*approx) << 1 + ((low*approx) >> 15) << 1;
	    temp = Integer.MAX_VALUE - temp;
	    temp1 = (short)(temp>>16);
	    temp2 = (short)((temp - temp1<<16)>>1);

	    temp = (temp1*approx + (temp2*approx)>> 15) << 1;
	    temp1 = (short)(temp>>16);
	    temp2 = (short)((temp - temp1<<16)>>1);

	    numHi = (short)(num<<16);
	    numLow = (short)((num - numHi<<16)>>1);

	    temp = numHi*temp1 + (numHi*temp2)>> 15 + (numLow*temp1) >> 15;
	    temp = temp<<3;
	    return temp;
	}

}
