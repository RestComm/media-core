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
package org.mobicents.media.server.impl.dsp.audio.g729;

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
