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

package org.restcomm.media.codec.g729;

public class DecLag {
	
	/*------------------------------------------------------------------------*
	 *    Function dec_lag3                                                   *
	 *             ~~~~~~~~                                                   *
	 *   Decoding of fractional pitch lag with 1/3 resolution.                *
	 * See "enc_lag3.c" for more details about the encoding procedure.        *
	 *------------------------------------------------------------------------*/

	public static void dec_lag3(     /* Decode the pitch lag                   */
	  int index,       /* input : received pitch index           */
	  int pit_min,     /* input : minimum pitch lag              */
	  int pit_max,     /* input : maximum pitch lag              */
	  int i_subfr,     /* input : subframe flag                  */
	  IntegerPointer T0,         /* output: integer part of pitch lag      */
	  IntegerPointer T0_frac     /* output: fractional part of pitch lag   */
	)
	{
	  int i;
	  int T0_min, T0_max;

	  if (i_subfr == 0)                  /* if 1st subframe */
	  {
	    if (index < 197)
	    {
	       T0.value = (index+2)/3 + 19;
	       T0_frac.value = index - T0.value*3 + 58;
	    }
	    else
	    {
	      T0.value = index - 112;
	      T0_frac.value = 0;
	    }
	  }

	  else  /* second subframe */
	  {
	    /* find T0_min and T0_max for 2nd subframe */

	    T0_min = T0.value - 5;
	    if (T0_min < pit_min)
	      T0_min = pit_min;

	    T0_max = T0_min + 9;
	    if(T0_max > pit_max)
	    {
	      T0_max = pit_max;
	      T0_min = T0_max -9;
	    }

	    i = (index+2)/3 - 1;
	    T0.value = i + T0_min;
	    T0_frac.value = index - 2 - i*3;
	  }
	  return;
	}

}
