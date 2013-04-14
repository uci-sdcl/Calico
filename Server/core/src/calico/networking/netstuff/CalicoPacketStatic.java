/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico.networking.netstuff;
import java.nio.*;

public class CalicoPacketStatic
{
	private ByteBuffer buf;
	
	public CalicoPacketStatic(int c)
	{
		buf = ByteBuffer.allocate(1400);
		buf.order( ByteOrder.LITTLE_ENDIAN );
		buf.putInt(c);
	}
	
	public CalicoPacketStatic(byte[] data)
	{
		buf = ByteBuffer.wrap(data);
		buf.order( ByteOrder.LITTLE_ENDIAN );
	}
	
	public void print()
	{
		byte[] bytes = export();
	
		for(int i=0;i<bytes.length;i++)
		{
			System.out.print( Integer.toString( (bytes[i] & 0xFF), 16).toUpperCase() + " ");
		}
		System.out.println( buf.toString() );
	}
	public void printString()
	{
		System.out.println( new String( export() ) );
	}
	
	
	public void putString(String str)
	{
		char[] chs = str.toCharArray();
	
		for(int i=0;i<chs.length;i++)
		{
			buf.putChar( chs[i] );
		}
		buf.putChar( Character.MIN_VALUE );
	
	}
	public String getString()
	{
		String str = new String();
		int len = buf.remaining();
	
		for(int i=0;i < len; i++)
		{
			try
			{
				char ch = buf.getChar();
				if(ch == Character.MIN_VALUE )
				{
					break;
				}
				else
				{
					str += ch;
				}
			}
			catch(Exception e){break;}
		}
	
		return str;
	}
	
	
	public void putInt(int i){buf.putInt(i);}
	public int getInt(){return buf.getInt();}
	public int getInt(int i){return buf.getInt(i);}
	
	public void putChar(char c){buf.putChar(c);}
	public char getChar(){return buf.getChar();}
	
	public void putFloat(float f){buf.putFloat(f);}
	public float getFloat(){return buf.getFloat();}
	
	public void putByte(byte b){buf.put(b);}
	public void putByte(byte[] b){buf.put(b);}
	public void putBytes(byte[] b){putByte(b);}
	public byte getByte(){return buf.get();}
	
	public void putShort(short s){buf.putShort(s);}
	public short getShort(){return buf.getShort();}
	
	public void putDouble(double d){buf.putDouble(d);}
	public double getDouble(){return buf.getDouble();}
	
	public void putLong(long l){buf.putLong(l);}
	public long getLong(){return buf.getLong();}
	
	public void putBoolean(boolean b)
	{
		if(b)
		{
			putByte( (byte) 1 );
		}
		else
		{
			putByte( (byte) 2 );
		}
	}
	public boolean getBoolean()
	{
		int b = (int) getByte();
		
		if(b==1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public byte[] export()
	{
		return buf.array();
	}


}
