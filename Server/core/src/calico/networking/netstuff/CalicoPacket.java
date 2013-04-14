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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import calico.components.CGroupImage;

/**
 * CalicoPacket is used for network communication and easily building packets
 * @author mdempsey
 *
 */
public class CalicoPacket
{

	private byte[] buffer = null;
	private int length = 0;
	private int position = 0;
	
	
	

	public CalicoPacket(int size)
	{
		this.buffer = new byte[size];
		this.length = size;
		this.position = 0;
	}
	public CalicoPacket()
	{
		this(50);
	}
	
	// COMMAND, (Starting Size)
	public CalicoPacket(int c, int s)
	{
		this(s);
		putInt(c);
	}
	
	public CalicoPacket(byte[] data)
	{
		this(data, false);
	}
	public CalicoPacket(byte[] data, boolean doNotCopy)
	{
		if(doNotCopy)
		{
			this.buffer = data;
			this.length = data.length;
			this.position = 0;
		}
		else
		{
			this.buffer = new byte[data.length];
			this.length = data.length;
			this.position = 0;
			System.arraycopy(data, 0, this.buffer, 0, data.length);
		}
	}
	public CalicoPacket(byte[] data, int boffset, int blength)
	{
		this(blength);
		System.arraycopy(data, boffset, this.buffer, 0, blength);
	}
	
	public void setPosition(int newpos)
	{
		this.position = newpos;
	}
	public void rewind()
	{
		this.position = 0;
	}
		
	public int getLength()
	{
		return length;
	}
	
	public int remaining()
	{
		return length - position;
	}
	
	private void increaseSizeIfNeeded(int size)
	{
		int increaseBy = remaining() - size;
		if(increaseBy<0)
		{
			increaseSize(Math.abs(increaseBy));
		}
	}
	
	
	private void increaseSize(int increaseBy)
	{
		byte[] tbuf = new byte[this.length+increaseBy];
		System.arraycopy(this.buffer, 0, tbuf, 0, this.length);
		
		this.length = tbuf.length;
		this.buffer = tbuf;
	}

	
	public void putString(String str)
	{
		try
		{
			byte[] strbytes = str.getBytes("UTF-8");

			increaseSizeIfNeeded( strbytes.length + ByteUtils.SIZE_OF_INT );
			
			putInt(strbytes.length);
			putBytes(strbytes);
		}
		catch(Exception e)
		{
			// wtf?
		}
	}
	public String getString()
	{
		int len = getInt();
		
		try
		{

			String str = new String(this.buffer, this.position, len,"UTF-8");
			this.position = this.position + len;
			return str;
		}
		catch(Exception e)
		{
			// wtf?
			return "";
		}
	}
	
	public void putImage(Image img)
	{
		byte[] imageByteArray = getImageByteArray(img);
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_INT + imageByteArray.length );
		
		putInt(imageByteArray.length);
		putByte(imageByteArray); 
		
	}
	
	public static byte [] getImageByteArray(Image image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(getBufferedImage(image), "PNG", baos);
        } catch (IOException ex) {
            //handle it here.... not implemented yet...
        }
        return baos.toByteArray();
    }
	
    private static BufferedImage getBufferedImage(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        //Graphics2D g2d = bi.createGraphics();
        bi.getGraphics().drawImage(image, 0, 0, null);
        return bi;
    }
	
	public BufferedImage getBufferedImage()
	{
		int len = getInt();
		
		byte[] imageByteArray = getByteArray(len);
		InputStream in = new ByteArrayInputStream(imageByteArray);
		BufferedImage bImage = null;
		try {
			bImage = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return bImage;
	}


	public void putInt(int intVal)
	{
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_INT );
		ByteUtils.writeInt(this.buffer, intVal, this.position);
		this.position = this.position + ByteUtils.SIZE_OF_INT;
	}
	public int getInt()
	{
		int temp = ByteUtils.readInt(this.buffer, this.position);
		this.position = this.position + ByteUtils.SIZE_OF_INT;
		return temp;
	}


	public void putFloat(float floatVal)
	{
		putInt(Float.floatToIntBits(floatVal));
	}
	public float getFloat()
	{
		return Float.intBitsToFloat(getInt());
	}

	public void putByte(byte byteVal)
	{
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_BYTE );
		this.buffer[this.position] = byteVal;
		this.position = this.position + ByteUtils.SIZE_OF_BYTE;
	}
	public void putByte(byte[] byteVal)
	{
		putBytes(byteVal);
	}
	public void putBytes(byte[] byteVal)
	{
		increaseSizeIfNeeded( byteVal.length );
		System.arraycopy(byteVal, 0, this.buffer, this.position, byteVal.length);
		this.position = this.position + byteVal.length;
	}
	public void putBytes(byte[] byteVal, int boffset, int blength)
	{
		increaseSizeIfNeeded( blength );
		System.arraycopy(byteVal, boffset, this.buffer, this.position, blength);
		this.position = this.position + blength;
	}
	public byte getByte()
	{
		byte temp = this.buffer[this.position];
		this.position = this.position + ByteUtils.SIZE_OF_BYTE;
		return temp;
	}
	public byte[] getByteArray(int len)
	{
		byte[] temp = new byte[len];
		
		System.arraycopy(this.buffer, this.position, temp, 0, len);
		
		this.position = this.position + len;
		
		return temp;
	}


	public void putDouble(double doubleVal)
	{
		putLong(Double.doubleToLongBits(doubleVal));
	}
	public double getDouble()
	{
		return Double.longBitsToDouble(getLong());
	}

	public void putLong(long longVal)
	{
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_LONG );
		ByteUtils.writeLong(this.buffer, longVal, this.position);
		this.position = this.position + ByteUtils.SIZE_OF_LONG;
	}
	public long getLong()
	{
		long temp = ByteUtils.readLong(this.buffer, this.position);
		
		this.position = this.position + ByteUtils.SIZE_OF_LONG;
		return temp;
	}
	
	
	public void putShort(short shortVal)
	{
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_SHORT );
		ByteUtils.writeShort(this.buffer, shortVal, this.position);
		this.position = this.position + ByteUtils.SIZE_OF_SHORT;
	}
	public short getShort()
	{
		short temp = ByteUtils.readShort(this.buffer, this.position);
		
		this.position = this.position + ByteUtils.SIZE_OF_SHORT;
		return temp;
	}
	
	
	public void putChar(char charVal)
	{
		increaseSizeIfNeeded( ByteUtils.SIZE_OF_CHAR );
		ByteUtils.writeChar(this.buffer, charVal, this.position);
		this.position = this.position + ByteUtils.SIZE_OF_CHAR;
	}
	public char getChar()
	{
		char temp = ByteUtils.readChar(this.buffer, this.position);
		
		this.position = this.position + ByteUtils.SIZE_OF_CHAR;
		return temp;
	}
	public int getCharInt()
	{
		return ( (int) getChar() );
	}
	public void putCharInt(int charIntVal)
	{
		putChar( (char) charIntVal );
	}
	
	
	
	public Color getColor()
	{
		int color = getInt();
		return new Color(color);
	}
	public void putColor(Color color)
	{
		putInt( color.getRGB() );
	}
	
	
	public boolean getBoolean()
	{
		int temp = (int) getByte();
		return (temp==1);
	}
	public void putBoolean(boolean bool)
	{
		if(bool)
		{
			putByte( (byte) 0x01 );	
		}
		else
		{
			putByte( (byte) 0x00 );
		}
	}
	
	
	
	/**
	 * Clones this buffer and returns it
	 * @return
	 */
	public byte[] export()
	{
		byte[] tmp = new byte[this.length];
		System.arraycopy(this.buffer, 0, tmp, 0, this.length);
		return tmp;
	}
	
	/**
	 * Clones this packet into a byte buffer prefixed with the size
	 * @return the byte array of this packet, prefixed with the size
	 */
	public byte[] exportWithSize()
	{
		byte[] newdata = new byte[this.length+ByteUtils.SIZE_OF_INT];

		ByteUtils.writeInt(newdata, this.length, 0);
		System.arraycopy(this.buffer, 0, newdata, ByteUtils.SIZE_OF_INT, this.length);
		return newdata;
	}
	
	/**
	 * This returns direct access to the buffer of the packet
	 * @return the buffer
	 */
	public byte[] getBuffer()
	{
		return this.buffer;
	}
	
	
	/**
	 * Returns the size of the buffer
	 * @return
	 */
	public int getBufferSize()
	{
		return this.length;
	}

	
	/**
	 * Get a hex string of the packet
	 * @return hex string
	 */
	public String printString()
	{
		return ByteUtils.toHexString(this.buffer);
	}
	
	public long getUUID()
	{
		CalicoPacket p = new CalicoPacket(this.buffer);
		
		int com = p.getInt();
		
		NetCommandFormat ncfmt = NetworkCommand.getFormat(com);
		
		String name = ncfmt.getName();
		
		if (com == NetworkCommand.STROKE_LOAD || com == NetworkCommand.GROUP_LOAD || com == NetworkCommand.ARROW_CREATE)
		{
			return p.getLong();
		}
		else
		{
			return -1;
		}
	}
	
	public long getCUUID()
	{
		CalicoPacket p = new CalicoPacket(this.buffer);
		
		int com = p.getInt();
		
		NetCommandFormat ncfmt = NetworkCommand.getFormat(com);
		
		String name = ncfmt.getName();
		
		if (com == NetworkCommand.STROKE_LOAD || com == NetworkCommand.GROUP_LOAD || com == NetworkCommand.ARROW_CREATE)
		{
			p.getLong();
			return p.getLong();
		}
		else
		{
			return 0;
		}
	}
	
	public int getCommand()
	{
		CalicoPacket p = new CalicoPacket(this.buffer);
		
		return p.getInt();
	}
	
	/**
	 * Prints this packet using the formatted output
	 */
	public String toString()
	{
		CalicoPacket p = new CalicoPacket(this.buffer);
		
		int com = p.getInt();
		
		NetCommandFormat ncfmt = NetworkCommand.getFormat(com);
		String name = ncfmt.getName();
		String fmt = ncfmt.getFormat();
		
		if(fmt.length()==0)
		{
			return name+"() ["+this.length+"]"; 
		}
		
		StringBuffer sbuf = new StringBuffer();
		sbuf.append( name );
		sbuf.append( "(" );
		
		try
		{
			
			
			for(int i=0;i<fmt.length();i++)
			{
				char fmtc = fmt.charAt(i);
				if(i!=0)
				{
					sbuf.append(",");
				}
				switch(fmtc)
				{
					case 'S':
						sbuf.append(p.getString());
						break;
					case 's':
						sbuf.append(p.getShort());
						break;
						
					case 'I':
						sbuf.append(p.getInt());
						break;
					case 'i':
						sbuf.append(p.getCharInt());
						break;
						
					case 'L':
						sbuf.append(p.getLong());
						break;
						
					case 'f':
						sbuf.append(p.getFloat());
						break;
						
					case 'd':
						sbuf.append(p.getDouble());
						break;
						
					case 'B':
						if(p.getBoolean())
						{
							sbuf.append("TRUE");
						}
						else
						{
							sbuf.append("FALSE");
						}
						break;
					case 'b':
						p.getByte();
						break;
						
					case 'C':
						Color tempcolor = new Color(p.getInt());
						sbuf.append(tempcolor.getRed()+":"+tempcolor.getGreen()+":"+tempcolor.getBlue());
						break;
					case 'c':
						sbuf.append(p.getChar());
						break;
				}
			}
			
			sbuf.append( ") ["+this.length+"]" );
			return sbuf.toString();
		}
		catch(Exception bue)
		{

			sbuf.append( "_ERROR_) ["+this.length+"]" );
			return sbuf.toString();
			//return name+"(_ERROR_) ["+this.length+"]";
		}
	}
	
	/**
	 * Used to statically create a packet for calico.
	 * @param com
	 * @param params
	 * @return
	 */
	public static CalicoPacket getPacket(int com, Object... params)
	{
		int size = getSizeOfPacket(params) + ByteUtils.SIZE_OF_INT;
		CalicoPacket p = new CalicoPacket(size);
		p.putInt(com);
		
		
		for(int i=0;i<params.length;i++)
		{
			if( params[i] instanceof Long )
			{
				p.putLong( ((Long) params[i]).longValue() );
			}
			else if( params[i] instanceof Integer)
			{
				p.putInt( ((Integer)params[i]).intValue() );
			}
			else if( params[i] instanceof String )
			{
				p.putString( (String) params[i] );
			}
			else if( params[i] instanceof Float )
			{
				p.putFloat( ((Float)params[i]).floatValue() );
			}
			else if( params[i] instanceof Double )
			{
				p.putDouble( ((Double)params[i]).doubleValue() );
			}
			else if( params[i] instanceof Color )
			{
				p.putInt( ((Color) params[i]).getRGB() );
			}
			else if( (params[i] instanceof Boolean) )
			{
				p.putBoolean( ((Boolean) params[i]).booleanValue() );
			}
			else if( (params[i] instanceof Byte) )
			{
				p.putByte( ((Byte) params[i]).byteValue() );
			}
			else if( (params[i] instanceof Image) )
			{
				p.putImage( (Image) params[i]);
			}
		}
		return p;
	}

	/**
	 * Pass a list of parts and see how big the packet will be
	 * @param parts
	 * @return
	 */
	public static int getSizeOfPacket(Object[] parts)
	{
		int size = 0;
		
		for(int i=0;i<parts.length;i++)
		{
			if ( (parts[i] instanceof Long) || (parts[i] instanceof Double) )
			{
				size = size + ByteUtils.SIZE_OF_LONG;
			}
			else if( (parts[i] instanceof Integer) || (parts[i] instanceof Float) || (parts[i] instanceof Color) )
			{
				size = size + ByteUtils.SIZE_OF_INT;
			}
			else if ( parts[i] instanceof String )
			{
				size = size + getSizeOfString((String)parts[i]);
			}
			else if ( (parts[i] instanceof Byte) || (parts[i] instanceof Boolean) )
			{
				size = size + ByteUtils.SIZE_OF_BYTE;
			}
			else if ( (parts[i] instanceof Image) )
			{
				size = size + getSizeOfImage((Image)parts[i]);
			}
		}
		return size;
	}
	
	public static int getSizeOfString(String str) {
		int size = 0;
		try
		{
			size = ByteUtils.SIZE_OF_INT + (str).getBytes("UTF-8").length;
		}
		catch(Exception e)
		{
			size = ByteUtils.SIZE_OF_INT;
		}
		return size;
	}
	
	public static int getSizeOfImage(Image img) {
		return ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_BYTE * getImageByteArray(img).length;
	}
	
	/**
	 * Returns a CalicoPacket with a command and no parameters
	 * @param com
	 * @return
	 */
	public static CalicoPacket command(int com)
	{
		CalicoPacket temp = new CalicoPacket(ByteUtils.SIZE_OF_INT);
		temp.putInt(com);
		return temp;
	}
	public void dispose() {
		buffer = null;
		
	}
	
	
}//
