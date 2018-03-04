/*
 * Copyright (C) 2006 Frederic Simon
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.stellarium.data;

import java.io.*;


/**
 * This is like <coe>java.io.DataInputStream</code>,
 * but this one is forcing the intel format, reversing byte order when reading numbers.
 *
 * @author Fred Simon
 * @see java.io.DataInputStream
 */
public class ReverseDataInputStream extends FilterInputStream implements DataInput {

    /**
     * Creates a ReverseDataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public ReverseDataInputStream(InputStream in) {
        super(in);
    }

    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    public final int skipBytes(int n) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
            total += cur;
        }

        return total;
    }

    public final boolean readBoolean() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }

    public final byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) ch;
    }

    public final int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short) ((ch2 << 8) + ch1);
    }

    public final int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch2 << 8) + ch1;
    }

    public final char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char) ((ch2 << 8) + ch1);
    }

    public final int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
    }

    private byte readBuffer[] = new byte[8];

    public final long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[7] << 56) +
                ((long) (readBuffer[6] & 255) << 48) +
                ((long) (readBuffer[5] & 255) << 40) +
                ((long) (readBuffer[4] & 255) << 32) +
                ((long) (readBuffer[3] & 255) << 24) +
                ((readBuffer[2] & 255) << 16) +
                ((readBuffer[1] & 255) << 8) +
                ((readBuffer[0] & 255)));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    public final String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }
}
