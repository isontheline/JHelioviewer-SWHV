package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class VTAO {

    protected final int[] attribLens;
    protected final VTBO[] vtbos;

    private int vaoID = -1;
    private boolean inited = false;

    VTAO(int[] _attribLens) {
        attribLens = _attribLens;
        vtbos = new VTBO[attribLens.length];
    }

    public void init(GL2 gl) {
        if (!inited) {
            inited = true;

            int[] tmpId = new int[1];
            gl.glGenVertexArrays(1, tmpId, 0);
            vaoID = tmpId[0];

            gl.glBindVertexArray(vaoID);
            for (int i = 0; i < attribLens.length; i++) {
                vtbos[i] = new VTBO(gl, GL2.GL_TEXTURE0 + i, attribLens[i]);
                gl.glEnableVertexAttribArray(i);
                gl.glVertexAttribPointer(i, attribLens[i], GL2.GL_FLOAT, false, 0, 0);
            }
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            inited = false;

            gl.glDeleteVertexArrays(1, new int[]{vaoID}, 0);
            vaoID = -1;

            for (int i = 0; i < vtbos.length; i++) {
                vtbos[i].delete(gl);
                vtbos[i] = null;
            }
        }
    }

    protected void bind(GL2 gl) {
        gl.glBindVertexArray(vaoID);
        for (VTBO vtbo : vtbos) {
            vtbo.bind(gl);
        }
    }

}
