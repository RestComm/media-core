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

package org.restcomm.media.spi.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Case insensitive text block representation.
 *
 * This class provides time deterministic and fast methods for creating
 * and comparing short character strings like format names, attribute values, etc.
 *
 * @author kulikov
 */
public class Text implements CharSequence {
    //reference on the local buffer
    protected byte[] chars;

    //the start position
    protected int pos;

    //the length of the string
    protected int len;

    /** points to the line separator */
    private int linePointer;

    private int hash = -1;
    
    private byte zero_byte=(byte)'0';
    private byte nine_byte=(byte)'9';
    private byte a_byte=(byte)'a';
    private byte f_byte=(byte)'f';
    private byte A_byte=(byte)'A';
    private byte F_byte=(byte)'F';
    private byte minus_byte=(byte)'-';
    
    /**
     * Create an empty text object.
     *
     * This object later can be strained on some memory area
     */
    public Text() {
    }

    /**
     * Creates new object with specified text.
     *
     * @param another the text object.
     */
    protected Text(Text another) {
        this.chars = another.chars;
        this.pos = another.pos;
        this.len = another.len;
    }
    
    /**
     * Creates new instance with specified text.
     *
     * @param s the text value.
     */
    public Text(String s) {
        this.chars = s.getBytes();
        this.pos = 0;
        this.len = chars.length;
    }

    /**
     * Creates new text matching to specified integer number.
     * 
     * @param i the integer number.
     */
    public Text(int i) {
        chars = new byte[10];
        int remainder = i;
        
        
        while (remainder >= 10) {
            chars[len++] = (byte)(remainder % 10 + 48);
            remainder /= 10;
        }
        
        chars[len++] = (byte)(remainder + 48);
        int n = len/ 2;
        
        byte b = 0;
        
        for (int k = 0; k < n; k++) {
            b = chars[len - 1 - k];
            chars[len - 1 - k] = chars[k];
            chars[k] = b;
        }
        
        
        pos = 0;
    }
    
    /**
     * Creates new instance and straining it into memory.
     * @param data memory
     * @param pos initial position
     * @param len the length from the initial position
     */
    public Text(byte[] data, int pos, int len) {
        this.chars = data;
        this.pos = pos;
        this.len = len;
    }

    /**
     * Strains this object into the memory area.
     *
     * @param data the memory area
     * @param pos the initial position
     * @param len the length of area to use
     */
    public void strain(byte[] data, int pos, int len) {
        this.chars = data;
        this.pos = pos;
        this.len = len;
        this.linePointer = 0;
    }

    /**
     * (Non Java-doc.)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return len;
    }

    /**
     * (Non Java-doc.)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        return (char) chars[pos + index];
    }

    /**
     * Copies reference to another text object.
     * 
     * @param destination the another text object
     */
    public void copy(Text destination) {
        destination.strain(chars, pos, len);
    }
    
    /**
     * Copies data from this buffer to another buffer.
     * 
     * @param destination the pointer to another buffer.
     */
    public void duplicate(Text destination) {
        System.arraycopy(chars, pos, destination.chars, destination.pos, len);
        destination.len = len;
    }
    
    /**
     * Divides text into parts using given separator and writes results
     * into the parts array.
     * 
     * @param separator the character used for splitting
     * @param parts the array used to hold parts of the text
     * @return the number of parts.
     */
    public int divide(char separator, Text[] parts) {
        int pointer = pos;
        int limit = pos + len;
        int mark = pointer;
        int count = 0;
        
        while (pointer < limit) {
            if (chars[pointer] == separator) {
                parts[count].strain(chars, mark, pointer - mark);
                mark = pointer + 1;
                count++;
                
                if (count == parts.length - 1) {
                    break;
                }
            }
            pointer++;
        }
        
        if(limit > mark)
        {
        	parts[count].strain(chars, mark, limit - mark);
        	count++;
        }
        
        return count;
    }
    
    public int divide(char[] separators, Text[] parts) {
        int pointer = pos;
        int limit = pos + len;
        int mark = pointer;
        int count = 0;
        int k = 0;
        
        while (pointer < limit) {
            if (chars[pointer] == separators[k]) {
                parts[count].strain(chars, mark, pointer - mark);
                mark = pointer + 1;
                k++;
                count++;
                
//                if (count == parts.length - 1) {
//                    break;
//                }
            }
            pointer++;
        }
        
        if (limit > mark) {
            parts[count].strain(chars, mark, limit - mark);
            count++;
        }
        
        return count;
    }
    
    /**
     * (Non Java-doc.)
     * @see java.lang.CharSequence#subSequence(int, int);
     */
    public CharSequence subSequence(int start, int end) {
        return subtext(start, end);
    }
    
    public Text subtext(int start, int end) {
    	return new Text(chars, this.pos + start, end - start);
    }
    
    public Text subtext(int start) {
    	return new Text(this.chars, this.pos + start, this.len);
    }

    /**
     * Splits text into partitions.
     * 
     * @param separator character used for partitioning
     * @return array of text strings.
     */
    public Collection<Text> split(char separator) {
        int pointer = pos;
        int limit = pos + len;
        int mark = pointer;

        ArrayList<Text> tokens = new ArrayList<Text>();
        while (pointer < limit) {
            if (chars[pointer] == separator) {
                tokens.add(new Text(chars, mark, pointer - mark));
                mark = pointer + 1;
            }
            pointer++;
        }
        
        tokens.add(new Text(chars, mark, limit - mark));        
        return tokens;
    }

    /**
     * Splits text into partitions.
     * 
     * @param separator character used for partitioning
     * @return array of text strings.
     */
    public Collection<Text> split(Text separator) {
    	ArrayList<Text> tokens = new ArrayList<Text>();
    	int pointer = pos;
        int limit = pos + len;
        int mark = pointer;
        
        if(separator.length()==0)
    	{
        	tokens.add(new Text(chars, mark, pointer - mark));
    		return tokens;
    	}
    	
        int index=0;
        while (pointer < limit) {
            if (chars[pointer] == separator.charAt(index))
                index++;
            else 
            	index=0;
                        
            if(index==separator.length())
            {
            	tokens.add(new Text(chars, mark, pointer - mark));
                mark = pointer + 1;
                index=0;
            }
            
            pointer++;                    
        }
        
        tokens.add(new Text(chars, mark, limit - mark));        
        return tokens;
    }
    
    public int indexOf(Text value) {
    	int pointer = pos;
        int limit = pos + len;
        int mark = pointer;
        
        if(value.length()==0)
    		return 0;
    	
        int index=0;
        while (pointer < limit) {
            if (chars[pointer] == value.charAt(index))
                index++;
            else 
            {
            	index=0;
            	mark++;
            	pointer=mark;
            }
            
            if(index==value.length())
            	return mark;
            
            pointer++;                    
        }
        
        return -1;
    }
    
    public int indexOf(char value) {
        int limit = this.pos + this.len;
        for (int pointer = this.pos; pointer < limit; pointer++) {
			if(value == this.chars[pointer]) {
				return pointer;
			}
		}
        return -1;
    }
    
    /**
     * Removes whitespace from the head and tail of the string.
     * 
     */
    public void trim() {
    	try
    	{
    	//cut white spaces from the head
		while (len>0 && chars[pos] == ' ') {
			pos++;
			len--;
		}
    	
        //cut from the end
        while (len>0 && (chars[pos + len - 1] == ' ' || chars[pos + len - 1] == '\n' || chars[pos + len - 1] == '\r')) {
            len--;
        }
    	}
    	catch(Exception e)
    	{
    		System.out.println("len:" + len + ",pos:" + pos);
    	}
    }

    /**
     * Extracts next line from this text.
     * 
     * @return
     */
    public Text nextLine() {
        if (linePointer == 0) {
            linePointer = pos;
        } else {
            linePointer++;
        }

        int mark = linePointer;
        int limit = pos + len;

        while (linePointer < limit && chars[linePointer] != '\n') {
            linePointer++;
        }
        
        return new Text(chars, mark, linePointer - mark);
    }

    /**
     * Shows is this text contains more lines.
     *
     * @return true if there are more lines 
     */
    public boolean hasMoreLines() {
        return linePointer < pos + len;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof Text)) {
            return false;
        }

        Text t = (Text) other;
        if (this.len != t.len) {
            return false;
        }

        //return this.hashCode() == t.hashCode();
        return compareChars(t.chars, t.pos);
    }
    
    @Override
    public int hashCode() {
        if (hash == -1) {
            hash = 67 * 7 + Arrays.hashCode(this.chars);
        }
        return hash;
    }

    /**
     * Compares specified character string with current string.
     * The upper and lower case characters are considered as equals.
     *
     * @param chars the string to be compared
     * @return true if specified string equals to current
     */
    private boolean compareChars(byte[] chars, int pos) {
        for (int i = 0; i < len; i++) {
            if (differentChars((char) this.chars[i + this.pos], (char) chars[i + pos])) return false;
        }
        return true;
    }
    
	/**
	 * Indicates whether the text starts with a certain pattern.
	 * 
	 * @param pattern
	 *            The pattern to match
	 * @return <code>true</code> if this text starts with the pattern.
	 *         Otherwise, or if the pattern is null, returns <code>false</code>.
	 * @author hrosa
	 */
    public boolean startsWith(Text pattern) {
    	if (pattern == null) {
    		return false;
    	}
    	return this.subSequence(0, pattern.len).equals(pattern);
    }

    /**
     * Compares two chars.
     *
     * @param c1 the first char
     * @param c2 the second char
     * @return true if first and second chars are different.
     */
    private boolean differentChars(char c1, char c2) {
        if (65 <= c1 && c1 < 97) {
            c1 +=32;
        }

        if (65 <= c2 && c2 < 97) {
            c2 +=32;
        }
        return c1 != c2;
    }

    @Override
    public String toString() {
        return new String(chars, pos, len).trim();
    }

    /**
     * Converts string value to integer
     * @return integer value
     */
    public int toInteger() throws NumberFormatException {
        int res = 0;
        byte currChar;
        int i=1;
        currChar=chars[pos];
        boolean isMinus=false;
        
        if(currChar==minus_byte)
        	isMinus=true;
        else if(currChar>=zero_byte && currChar<=nine_byte)
    		res+=currChar-zero_byte;
    	else
    		throw new NumberFormatException("value is not numeric");
    	
        for (; i < len; i++) {
        	currChar=chars[pos+i];        		
        	if(currChar>=zero_byte && currChar<=nine_byte)
        	{
        		res*=10;
        		res+=currChar-zero_byte;
        	}        
        	else
        		throw new NumberFormatException("value is not numeric");
        }
        
        if(isMinus)
        	return 0-res;
        else
        	return res;
    }
    
    /**
     * Converts string value to long
     * @return long value
     */
    public long toLong() throws NumberFormatException {
        long res = 0;
        byte currChar;
        int i=1;
        currChar=chars[pos];
        boolean isMinus=false;
        
        if(currChar==minus_byte)
        	isMinus=true;
        else if(currChar>=zero_byte && currChar<=nine_byte)
    		res+=currChar-zero_byte;
    	else
    		throw new NumberFormatException("value is not numeric");
    	
        for (; i < len; i++) {
        	currChar=chars[pos+i];        		
        	if(currChar>=zero_byte && currChar<=nine_byte)
        	{
        		res*=10;
        		res+=currChar-zero_byte;
        	}        
        	else
        		throw new NumberFormatException("value is not numeric");
        }
        
        if(isMinus)
        	return 0-res;
        else
        	return res;
    }

    public int hexToInteger() throws NumberFormatException {
    	int res = 0;
    	byte currChar;
        for (int i = 0; i < len; i++) {
        	res<<=4;
        	currChar=chars[pos+i];
        	if(currChar>=zero_byte && currChar<=nine_byte)
        		res|=currChar-zero_byte;
        	else if(currChar>=a_byte && currChar<=f_byte)
        		res|=currChar-a_byte+10;
        	else if(currChar>=A_byte && currChar<=F_byte)
        		res|=currChar-a_byte+10;
        	else 
        	{
        		System.out.println("THROWING 1");
        		throw new NumberFormatException("value is not numeric");
        	}
        }
        
        return res;
    }
    
    /**
     * 10^a
     * 
     * @param a the power
     * @return the value of 10^a
     */
    private int pow(int a) {
        int res = 1;
        for (int i = 0; i < a; i++) {
            res *= 10;
        }
        return res;
    }

    /**
     * Converts character to digit.
     *
     * @param pos the position of the character
     * @return the digit value
     */
    private int digit(int pos) {
        return chars[pos] - 48;
    }        
    
    /**
     * Writes this text into byte buffer.
     * 
     * @param buffer the buffer for writing.
     */
    public void write(ByteBuffer buffer) {
        buffer.put(chars, pos, len);
    }
    
    /**
     * Copies substring from the current line upto the end to the specified destination.
     * 
     * @param other the destination object
     */
    public void copyRemainder(Text other) {
        other.chars = this.chars;
        other.pos = this.linePointer + 1;
        other.len = this.len - this.linePointer - 1;
    }
    
    /**
     * Checks does specified symbol is present in this text.
     * 
     * @param c the character to verify.
     * @return true if character c is presented in this text and false otherwise.
     */
    public boolean contains(char c) {
        for (int k = pos; k < len; k++) {
            if (chars[k] == c) return true;
        }
        return false;
    }
}
