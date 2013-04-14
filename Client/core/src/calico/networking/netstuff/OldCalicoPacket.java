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
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;


public class OldCalicoPacket
{
	private static final int DEFAULT_LENGTH = 4;
	private ByteBuffer buf;
	private int length = 4;
	private int bytesUsed = 0;

	public OldCalicoPacket(int c)
	{
		this(c, DEFAULT_LENGTH);
	}
	public OldCalicoPacket()
	{
		buf = ByteBuffer.allocate(length);
		buf.order( ByteOrder.LITTLE_ENDIAN );
	}
	
	// COMMAND, (Starting Size)
	public OldCalicoPacket(int c, int s)
	{
		length = s;
		buf = ByteBuffer.allocate(length);
		buf.order( ByteOrder.LITTLE_ENDIAN );
		putInt(c);
	}
	
	public OldCalicoPacket(byte[] data)
	{
		this();
		putBytes(data);
		rewind();
	}
	
	public void preallocate(int size)
	{
		increaseSize(size);
	}
	
	public void rewind()
	{
		buf.rewind();
	}
	
	public int getLength()
	{
		return length;
	}
	
	public int remaining()
	{
		return buf.remaining();
	}
	
	private void increaseSizeIfNeeded(int size)
	{
		int increaseBy = remaining() - size;
		bytesUsed = bytesUsed + size;
		if(increaseBy<0)
		{
			increaseSize(increaseBy);
		}
	}
	private void increaseSize(int increaseBy)
	{
		increaseBy = Math.abs(increaseBy);
		length = length + increaseBy;

		int bufpos = buf.position();
		
		ByteBuffer newbuf = ByteBuffer.allocate(length);
		newbuf.order(ByteOrder.LITTLE_ENDIAN);
		buf.rewind();
		newbuf.put(buf);
		newbuf.position(bufpos);
		
		buf = newbuf;
		/*
		byte[] tmp = buf.array();
		buf.clear();
		buf = ByteBuffer.allocate(tmp.length+increaseBy);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put(tmp);
		buf.position(bufpos);
		*/
	}

	public void putString(CharSequence str)
	{
		increaseSizeIfNeeded( ((str.length())*2) + 2 );

		for(int i=0;i<str.length();i++)
		{
			buf.putChar( str.charAt(i) );
		}
		buf.putChar( Character.MIN_VALUE );
	}
	
	
	public void putString(String str)
	{
		try
		{
			byte[] strbytes = str.getBytes("UTF-8");
			
			int size = strbytes.length;

			increaseSizeIfNeeded( size+4 );
			
			buf.putInt(size);
			buf.put(strbytes);
		}
		catch(Exception e)
		{
			// wtf?
		}
		/*
		char[] chs = str.toCharArray();
		increaseSizeIfNeeded( ((chs.length)*2) + 2 );

		for(int i=0;i<chs.length;i++)
		{
			buf.putChar( chs[i] );
		}
		buf.putChar( Character.MIN_VALUE );
		*/
	}
	public String getString()
	{
		int len = buf.getInt();
		byte[] strbytes = new byte[len];
		buf.get(strbytes);
		
		String str = null;
		try
		{
			str = new String(strbytes,"UTF-8");
			return str;
		}
		catch(Exception e)
		{
			// wtf?
			return "";
		}
		
		/*String str = new String();
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

		return str;*/
	}


	public void putInt(int i)
	{
		increaseSizeIfNeeded( 4 );
		buf.putInt(i);
	}
	public int getInt()
	{
		return buf.getInt();
	}

	public void putChar(char c)
	{
		increaseSizeIfNeeded( 2 );
		buf.putChar(c);
	}
	public char getChar()
	{
		return buf.getChar();
	}

	public void putFloat(float f)
	{
		increaseSizeIfNeeded( 4 );
		buf.putFloat(f);
	}
	public float getFloat()
	{
		return buf.getFloat();
	}

	public void putByte(byte b)
	{
		increaseSizeIfNeeded( 1 );
		buf.put(b);
	}
	public void putByte(byte[] b)
	{
		increaseSizeIfNeeded( b.length );
		buf.put(b);
	}
	public void putBytes(byte[] b)
	{
		putByte(b);
	}
	public byte getByte()
	{
		return buf.get();
	}

	public void putShort(short s)
	{
		increaseSizeIfNeeded( 2 );
		buf.putShort(s);
	}
	public short getShort()
	{
		return buf.getShort();
	}

	public void putDouble(double d)
	{
		increaseSizeIfNeeded( 8 );
		buf.putDouble(d);
	}
	public double getDouble()
	{
		return buf.getDouble();
	}

	public void putLong(long l)
	{
		increaseSizeIfNeeded( 8 );
		buf.putLong(l);
	}
	public long getLong()
	{
		return buf.getLong();
	}
	
	public int getBytesUsed()
	{
		return bytesUsed;
	}
	
	public byte[] export()
	{
		byte[] tmp = new byte[bytesUsed];
		byte[] cur = buf.array();
		
		
		System.arraycopy(cur, 0, tmp, 0, bytesUsed);

		return tmp;
	}
	public byte[] exportWithSize()
	{
		/*
		ByteBuffer tmp = ByteBuffer.allocate(bytesUsed+4);
		tmp.order(ByteOrder.LITTLE_ENDIAN);
		
		tmp.putInt(bytesUsed);
		tmp.put(buf.array());
		
		return tmp.array();
		*/
		ByteBuffer tmp = ByteBuffer.allocate(4);
		tmp.order(ByteOrder.LITTLE_ENDIAN);
		tmp.putInt(bytesUsed);
		
		byte[] newdata = new byte[bytesUsed+4];

		System.arraycopy(tmp.array(), 0, newdata, 0, 4);
		System.arraycopy(buf.array(), 0, newdata, 4, bytesUsed);
		return newdata;
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
	public String toString()
	{
		OldCalicoPacket p = new OldCalicoPacket(buf.array());
		p.rewind();
		int com = p.getInt();
		
		NetCommandFormat ncfmt = NetworkCommand.getFormat(com);
		String name = ncfmt.getName();
		String fmt = ncfmt.getFormat();
		
		if(fmt.length()==0)
		{
			return name+"() ["+getBytesUsed()+"]"; 
		}
		
		try
		{
			StringBuffer sbuf = new StringBuffer();
			sbuf.append( name );
			sbuf.append( "(" );
			
			for(int i=0;i<fmt.length();i++)
			{
				char fmtc = fmt.charAt(i);
				if(i!=0)
				{
					sbuf.append(",");
				}
				switch(fmtc)
				{
					case 's':
						sbuf.append(p.getString());
						break;
					case 'i':
						sbuf.append(p.getInt());
						break;
					case 'l':
						sbuf.append(p.getLong());
						break;
					case 'c':
						sbuf.append(p.getChar());
						break;
					case 'f':
						sbuf.append(p.getFloat());
						break;
					
				}
			}
			
			sbuf.append( ") ["+getBytesUsed()+"]" );
			return sbuf.toString();
		}
		catch(BufferUnderflowException bue)
		{
			return name+"(ERROR PARSING THIS) ["+getBytesUsed()+"]";
		}
	}
	
	/**
	 * Used to statically create a packet for calico.
	 * @param com
	 * @param params
	 * @return
	 */
	public static OldCalicoPacket getPacket(int com, Object... params)
	{
		int size = getSizeOfPacket(params) + 4;
		OldCalicoPacket p = new OldCalicoPacket(com, size);
		
		//long start = System.nanoTime();
		
		for(int i=0;i<params.length;i++)
		{
			if ( params[i] instanceof Long )
			{
				p.putLong( ((Long) params[i]).longValue() );
			}
			else if( params[i] instanceof Integer)
			{
				p.putInt( ((Integer)params[i]).intValue() );
			}
			else if ( params[i] instanceof String )
			{
				p.putString( (String) params[i] );
			}
			else if ( params[i] instanceof Float )
			{
				p.putFloat( ((Float)params[i]).floatValue() );
			}
			else if ( params[i] instanceof Double )
			{
				p.putDouble( ((Double)params[i]).doubleValue() );
			}
			else if ( params[i] instanceof Short )
			{
				p.putShort( ((Short) params[i]).shortValue() );
			}
			else if ( params[i] instanceof Character )
			{
				p.putChar( ((Character) params[i]).charValue() );
			}
			else if ( params[i] instanceof Byte )
			{
				p.putByte( ((Byte) params[i]).byteValue() );
			}
		}
		return p;
	}

	public static int getSizeOfPacket(Object[] parts)
	{
		int size = 0;
		
		for(int i=0;i<parts.length;i++)
		{
			if ( parts[i] instanceof Long )
			{
				size = size + 8;
			}
			else if( parts[i] instanceof Integer)
			{
				size = size + 4;
			}
			else if ( parts[i] instanceof String )
			{
				try
				{
					byte[] temp = ((String)parts[i]).getBytes("UTF-8");
					size = size + 4 + temp.length;
				}
				catch(Exception e)
				{
					size = size + 4;
				}
				//size = size + (2 * ((String)parts[i]).toCharArray().length ) + 2;
			}
			else if ( parts[i] instanceof Float )
			{
				size = size + 4;
			}
			else if ( parts[i] instanceof Double )
			{
				size = size + 8;
			}
			else if ( parts[i] instanceof Short )
			{
				size = size + 2;
			}
			else if ( parts[i] instanceof Character )
			{
				size = size + 2;
			}
			else if ( parts[i] instanceof Byte )
			{
				size = size + 1;
			}
		}
		return size;
	}
	
	
	
	
}// Calico Packet


