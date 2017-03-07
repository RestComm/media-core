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

package org.restcomm.media.codec.gsm;

/**
 * 
 * @author oifa yulian
 *
 */
public class BasicFunctions {
	
	public static final short A[]={20480,20480,20480,20480,13964,15360,8534,9036};
	public static final short B[]={0,0,2048,-2560,94,-1792,-341,-1144};
	public static final short MIC[]={-32,-32,-16,-16,-8,-8,-4,-4};
	public static final short MAC[]={31,31,15,15,7,7,3,3};
	public static final short INVA[]={13107,13107,13107,13107,19223,17476,31454,29708};
	
	public static final short DLB[]={6554,16384,26214,32767};
	public static final short QLB[]={3277,11469,21299,32767};
	
	public static final short H[]={-134,-374,0,2054,5741,8192,5741,2054,0,-374,-134};
	
	public static final short NRFAC[]={29128,26215,23832,21846,20165,18725,17476,16384};
	public static final short FAC[]={18431,20479,22527,24575,26623,28671,30719,32767};
	
	public BasicFunctions() {
    }
    
    public static short checkOverflow(int value)
    {
    	if(value<Short.MIN_VALUE)
    		return Short.MIN_VALUE;
    	else if(value>Short.MAX_VALUE)
    		return Short.MAX_VALUE;
    	
    	return (short)value;
    }
    
    public static int checkIntOverflow(long value)
    {
    	if(value<Integer.MIN_VALUE)
    		return Integer.MIN_VALUE;
    	else if(value>Integer.MAX_VALUE)
    		return Integer.MAX_VALUE;
    	
    	return (int)value;
    }
    
    public static short add(short var1,short var2)
    {
    	return checkOverflow((int)var1+(int)var2);
    }
    
    public static short sub(short var1,short var2)
    {
    	return checkOverflow((int)var1-(int)var2);
    }
    
    public static short mult(short var1,short var2)
    {
    	if(var1==Short.MIN_VALUE && var2==Short.MIN_VALUE)
    		return Short.MAX_VALUE;
    	
    	return (short) (((int)var1*(int)var2)>>15);
    }
    
    public static short mult_r(short var1,short var2)
    {
    	if(var1==Short.MIN_VALUE && var2==Short.MIN_VALUE)
    		return Short.MAX_VALUE;
    	
    	return (short) (((int)var1*(int)var2 + 16384)>>15);
    }

    public static short abs(short var1)
    {
    	if(var1==Short.MIN_VALUE)
    		return Short.MAX_VALUE;
    	else if(var1<0)
    		return (short)-var1;
    	
    	return (short)var1;
    }
    
    public static short div(short var1,short var2)
    {
    	if(var1<=0)
    		throw new ArithmeticException("Variable 1 should be positive");
    	
    	if(var2<var1)
    		throw new ArithmeticException("Variable 2 should be greater or equal to variable 1");
    	
    	int value1=var1;
    	int value2=var2;
    	short result=0;
    	for(int i=0;i<15;i++)
    	{
    		result<<=1;
    		value1<<=1;
    		if(value1>=value2)
    		{
    			value1=L_sub(value1,value2);
    			result=add(result,(short)1);    			
    		}
    	}
    	
    	return result;
    }
    
    public static int L_mult(short var1,short var2)
    {
    	return ((int)var1*(int)var2)<<1;
    }
    
    public static int L_add(int var1,int var2)
    {
    	return checkIntOverflow((long)var1+(long)var2);    	
    }
    
    public static int L_sub(int var1,int var2)
    {
    	return checkIntOverflow((long)var1-(long)var2);  
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
}
