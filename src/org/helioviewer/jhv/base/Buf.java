package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.helioviewer.jhv.math.Vec3;

public class Buf {

    private final ByteBuf buf;
    private final float[] last = new float[4];

    private int floats;
    private int bytes;

    public Buf(int len) {
        buf = Unpooled.directBuffer(len);
    }

    public void put2f(float x, float y) {
        buf.writeFloatLE(x);
        buf.writeFloatLE(y);
        floats += 2;
    }

    public void put4f(Vec3 v) {
        put4f((float) v.x, (float) v.y, (float) v.z, 1);
    }

    public void put4f(float x, float y, float z, float w) {
        last[0] = x;
        last[1] = y;
        last[2] = z;
        last[3] = w;
        repeat4f();
    }

    public void repeat4f() {
        buf.writeFloatLE(last[0]);
        buf.writeFloatLE(last[1]);
        buf.writeFloatLE(last[2]);
        buf.writeFloatLE(last[3]);
        floats += 4;
    }

    public void put4b(byte[] b) {
        buf.writeBytes(b, 0, 4);
        bytes++;
    }

    public void clear() {
        buf.setIndex(0, 0);
    }

    public ByteBuffer toBuffer() {
        return buf.nioBuffer();
    }

}