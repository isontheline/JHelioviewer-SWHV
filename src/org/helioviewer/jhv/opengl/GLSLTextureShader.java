package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

import org.helioviewer.jhv.math.Transform;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/glsl/texture.vert", "/glsl/texture.frag");

    private int refModelViewProjectionMatrix;
    private int colorRef;

    private float[] color = {1, 1, 1, 1};

    private GLSLTextureShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        texture._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        texture._dispose(gl);
    }

    @Override
    protected void bindAttribs(GL2 gl) {
        gl.glBindAttribLocation(progID, 0, "Vertex");
        gl.glBindAttribLocation(progID, 1, "Coord");
    }

    @Override
    protected void _after_init(GL2 gl) {
        bind(gl);
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        colorRef = gl.glGetUniformLocation(progID, "color");

        setTextureUnit(gl, "image", GLTexture.Unit.ZERO);
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    void setColor(float[] _color) {
        color = _color;
    }

}
