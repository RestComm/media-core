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
package org.mobicents.media.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import org.mobicents.media.format.AudioFormat;

/**
 *
 * @author Oleg Kulikov
 */
public class Utils {

    /** Creates a new instance of Utils */
    public Utils() {
    }

    /**
     * Creates audio format object from given format description.
     *
     * @param formatDesc the description of the format. Format description is 
     * as follows: codec, sampleRate Hz, sampleSize-bits, channels.
     * example: G729, 8000.0 Hz, 8-bits, Mono
     */
    public static AudioFormat parseFormat(String formatDesc) {
        String tokens[] = formatDesc.split(",");

        if (tokens.length != 4) {
            throw new IllegalArgumentException("Invalid format definition: " + formatDesc);
        }

        String encoding = tokens[0];

        String srDesc = tokens[1].substring(0, tokens[1].indexOf("Hz"));
        double sampleRate = Double.parseDouble(srDesc.trim());

        String szDesc = tokens[2].substring(0, tokens[2].indexOf("-bits"));
        int sampleSize = Integer.parseInt(szDesc.trim());

        int channels = 1;
        if (tokens[3].trim().equals("Mono")) {
            channels = 1;
        } else if (tokens[3].trim().equals("Stereo")) {
            channels = 2;
        } else {
            throw new IllegalArgumentException("Invalid format description: " + tokens[3]);
        }

        return new AudioFormat(encoding, sampleRate, sampleSize, channels);
    }

    public static String doMessage(Throwable t) {
        StringBuffer sb = new StringBuffer();
        int tick = 0;
        Throwable e = t;
        do {
            StackTraceElement[] trace = e.getStackTrace();
            if (tick++ == 0) {
                sb.append(e.getClass().getCanonicalName() + ":" + e.getLocalizedMessage() + "\n");
            } else {
                sb.append("Caused by: " + e.getClass().getCanonicalName() + ":" + e.getLocalizedMessage() + "\n");
            }
            for (StackTraceElement ste : trace) {
                sb.append("\t" + ste + "\n");
            }
            e = e.getCause();
        } while (e != null);

        return sb.toString();

    }
    
    public static final String _FILE_SCHEME_ = "file";
    public static final String _FILE_SCHEME_CONCAT_ = "file:/";
    //check if there is scheme;
    private final static Pattern schemePattern = Pattern.compile("\\w\\w+:.*");
    //windows file: g:/xxas/asd
    private final static Pattern windowsFilePattern = Pattern.compile("\\w:.*");
    
    /**
     * 
     * @param parrent parent uri to be used if string parameter is not absolute.
     * @param passedURI - uri passed to certain method, it will either form URI or be part of URI with <b>parent</b> parameter
     * @return uri to be used.
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public final static URL getAbsoluteURL(String parrent, String passedURI) throws URISyntaxException, MalformedURLException
    {
    	
        
        if(schemePattern.matches(schemePattern.pattern(), passedURI))
        {
        	//its a valid URI
        	return new URL(passedURI);
        }else if(windowsFilePattern.matches(windowsFilePattern.pattern(), passedURI) || passedURI.startsWith("/"))
        {
        	//its win or nix, its still absolute
        	return new URL(_FILE_SCHEME_CONCAT_+passedURI);
        }else
        {
        	String absPath = parrent + passedURI;
        	File f = new File(absPath);
        	//its not absolute, lets use parent
        	URL l = f.toURL();
        	return l;
        }
    }
    
    public static void addObject(Object[] target,Object o)
    {
    	for(int index = 0;index<target.length;index++)
    	{
    		if(target[index]== null)
    		{
    			target[index] = o;
    			return;
    		}
    	}
    	throw new IllegalStateException("Too many objects!");
    }
    public static boolean removeObject(Object[] target,Object o)
    {
    	 //listeners.remove(listener);
    	for(int index = 0;index<target.length;index++)
    	{
    		if(target[index]== o)
    		{
    			target[index] = null;
    			return true;
    		}
    	}
    	return false;
    }
}
