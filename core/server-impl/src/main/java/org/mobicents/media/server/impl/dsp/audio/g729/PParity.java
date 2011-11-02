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

public class PParity {

	/*-----------------------------------------------------*
	 * parity_pitch - compute parity bit for first 6 MSBs  *
	 *-----------------------------------------------------*/

	public static int parity_pitch(     /* output: parity bit (XOR of 6 MSB bits)     */
	  int pitch_index     /* input : index for which parity is computed */
	)
	{
	    int temp, sum, i, bit;

	    temp = pitch_index >> 1;

	    sum = 1;
	    for (i = 0; i <= 5; i++) {
	        temp >>= 1;
	        bit = temp & 1;
	        sum = sum + bit;
	    }
	    sum = sum & 1;
	    return (sum);
	}

	/*--------------------------------------------------------------------*
	 * check_parity_pitch - check parity of index with transmitted parity *
	 *--------------------------------------------------------------------*/

	public static int check_parity_pitch(  /* output: 0 = no error, 1= error */
	  int pitch_index,       /* input : index of parameter     */
	  int parity             /* input : parity bit             */
	)
	{
	    int temp, sum, i, bit;
	    temp = pitch_index >> 1;

	    sum = 1;
	    for (i = 0; i <= 5; i++) {
	        temp >>= 1;
	        bit = temp & 1;
	        sum = sum + bit;
	    }
	    sum += parity;
	    sum = sum & 1;
	    return (sum);
	}

}
