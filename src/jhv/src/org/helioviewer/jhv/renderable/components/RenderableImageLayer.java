package org.helioviewer.jhv.renderable.components;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.base.Pair;
import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.filters.FiltersPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLImage;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLShader;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class RenderableImageLayer implements Renderable {

    private static boolean showCorona = true;
    private static int nextLayerId = 0;
    private final int layerId;

    public int getLayerId() {
        return layerId;
    }

    public double minZ = -Sun.Radius;
    public double maxZ = Sun.Radius;

    private final int resolution = 3;
    private final GL3DVec2d[] pointlist = new GL3DVec2d[(resolution + 1) * 2 * 2];
    private int positionBufferID;
    private int indexBufferID;
    private int indexBufferSize;

    private int positionBufferSize;
    private final AbstractView mainLayerView;
    private final RenderableType type;
    private boolean isVisible = true;

    private final GLImage glImage;

    public RenderableImageLayer(AbstractView view) {
        type = new RenderableImageType(view.getName());
        layerId = nextLayerId++;
        mainLayerView = view;

        glImage = new GLImage();
        glImage.setLUT(((JHVJP2View) view).getStartLUT(), false);

        int count = 0;
        for (int i = 0; i <= resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                pointlist[count] = new GL3DVec2d(2. * (1. * i / resolution - 0.5), -2. * (j - 0.5));
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= resolution; j++) {
                pointlist[count] = new GL3DVec2d(2. * (i / 1. - 0.5), -2. * (1. * j / resolution - 0.5));
                count++;
            }
        }

        ImageViewerGui.getRenderableContainer().addBeforeRenderable(this);

        float opacity = (float) (1. / (1. + Layers.getNumLayers()));
        if (mainLayerView instanceof JHVJP2View) {
            JHVJP2View jp2v = (JHVJP2View) mainLayerView;
            if (jp2v.getName().contains("LASCO") || jp2v.getName().contains("COR")) {
                opacity = 1.f;
            }
        }
        glImage.setOpacity(opacity);
    }

    @Override
    public void init(GL2 gl) {
        glImage.init(gl);
        Pair<FloatBuffer, IntBuffer> bufferPair = makeIcosphere(2);
        FloatBuffer positionBuffer = bufferPair.a;
        IntBuffer indexBuffer = bufferPair.b;

        positionBufferSize = positionBuffer.capacity();
        positionBufferID = generate(gl);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

        indexBufferID = generate(gl);
        indexBufferSize = indexBuffer.capacity();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);

        Displayer.getActiveCamera().updateCameraTransformation();
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void updateROI() {
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = Double.MIN_VALUE;
        double maxPhysicalY = Double.MIN_VALUE;

        GL3DCamera activeCamera = Displayer.getActiveCamera();
        GL3DQuatd camdiff = getCameraDifferenceRotationQuatd(activeCamera, imageData);

        for (int i = 0; i < pointlist.length; i++) {
            GL3DVec3d hitPoint;
            hitPoint = activeCamera.getVectorFromSphereOrPlane(pointlist[i], camdiff);
            if (hitPoint != null) {
                minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
            }
        }

        double widthxAdd = 0; // Math.abs((maxPhysicalX - minPhysicalX) * 0.025);
        double widthyAdd = 0; //Math.abs((maxPhysicalY - minPhysicalY) * 0.025);
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        MetaData metaData = mainLayerView.getMetaData();
        GL3DVec2d metPhysicalSize = metaData.getPhysicalSize();
        double metLLX = metaData.getPhysicalLowerLeft().x;
        double metLLY = metaData.getPhysicalLowerLeft().y;
        double metURX = metLLX + metPhysicalSize.x;
        double metURY = metLLY + metPhysicalSize.y;

        if (minPhysicalX < metLLX)
            minPhysicalX = metLLX;
        if (minPhysicalY < metLLY)
            minPhysicalY = metLLY;
        if (maxPhysicalX > metURX)
            maxPhysicalX = metURX;
        if (maxPhysicalY > metURY)
            maxPhysicalY = metURY;

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;
        Region newRegion;
        if (regionWidth > 0 && regionHeight > 0) {
            newRegion = new Region(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
        } else {
            newRegion = new Region(metLLX, metLLY, metURX - metLLX, metURY - metLLY);
        }
        mainLayerView.setRegion(newRegion);

        Viewport layerViewport = new Viewport(Displayer.getViewportWidth(), Displayer.getViewportHeight());
        mainLayerView.setViewport(layerViewport);
    }

    private GL3DQuatd getCameraDifferenceRotationQuatd(GL3DCamera camera, ImageData imageData) {
        if (imageData == null)
            return new GL3DQuatd();

        GL3DQuatd cameraDifferenceRotation = camera.getRotation().copy();
        cameraDifferenceRotation.rotateWithConjugate(imageData.getMetaData().getRotationObs());

        return cameraDifferenceRotation;
    }

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        GLSLShader.bind(gl);
        {
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);

            glImage.applyFilters(gl, imageData, mainLayerView.getPreviousImageData(), mainLayerView.getBaseDifferenceImageData());

            GLSLShader.setViewport(GLInfo.pixelScale[0] * Displayer.getViewportWidth(), GLInfo.pixelScale[1] * Displayer.getViewportHeight());
            if (!RenderableImageLayer.showCorona) {
                GLSLShader.setOuterCutOffRadius(1.);
            }
            GLSLShader.filter(gl);

            GL3DCamera camera = Displayer.getActiveCamera();
            GL3DMat4d vpmi = camera.getOrthoMatrixInverse();
            vpmi.translate(new GL3DVec3d(-camera.getTranslation().x, -camera.getTranslation().y, 0.));
            GLSLShader.bindMatrix(gl, vpmi.getFloatArray());
            GLSLShader.bindCameraDifferenceRotationQuat(gl, getCameraDifferenceRotationQuatd(camera, imageData));
            if (glImage.getBaseDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, getCameraDifferenceRotationQuatd(camera, mainLayerView.getBaseDifferenceImageData()));
            } else if (glImage.getDifferenceMode()) {
                GLSLShader.bindDiffCameraDifferenceRotationQuat(gl, getCameraDifferenceRotationQuatd(camera, mainLayerView.getPreviousImageData()));
            }

            enablePositionVBO(gl);
            enableIndexVBO(gl);
            {
                gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * Buffers.SIZEOF_FLOAT, 0);
                GLSLShader.bindIsDisc(gl, 0);

                gl.glDepthRange(1.f, 1.f);
                gl.glDrawElements(GL2.GL_TRIANGLES, 6, GL2.GL_UNSIGNED_INT, (indexBufferSize - 6) * Buffers.SIZEOF_INT);
                gl.glDepthRange(0.f, 1.f);

                GLSLShader.bindIsDisc(gl, 1);
                gl.glDrawElements(GL2.GL_TRIANGLES, indexBufferSize - 6, GL2.GL_UNSIGNED_INT, 0);
            }
            disableIndexVBO(gl);
            disablePositionVBO(gl);

            gl.glColorMask(true, true, true, true);
            gl.glDisable(GL2.GL_CULL_FACE);
        }
        GLSLShader.unbind(gl);

        updateROI();
    }

    private int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    private void enableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
    }

    private void disableIndexVBO(GL2 gl) {
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void enablePositionVBO(GL2 gl) {
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
    }

    private void disablePositionVBO(GL2 gl) {
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void deletePositionVBO(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
    }

    private void deleteIndexVBO(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { indexBufferID }, 0);
    }

    @Override
    public void remove(GL2 gl) {
        Layers.removeLayer(mainLayerView);
        dispose(gl);
    }

    private static Pair<FloatBuffer, IntBuffer> makeIcosphere(int level) {
        float t = (float) ((Math.sqrt(5) - 1) / 2);
        float[][] icosahedronVertexList = new float[][] { new float[] { -1, -t, 0 }, new float[] { 0, 1, t }, new float[] { 0, 1, -t }, new float[] { 1, t, 0 }, new float[] { 1, -t, 0 }, new float[] { 0, -1, -t }, new float[] { 0, -1, t }, new float[] { t, 0, 1 }, new float[] { -t, 0, 1 }, new float[] { t, 0, -1 }, new float[] { -t, 0, -1 }, new float[] { -1, t, 0 }, };
        for (float[] v : icosahedronVertexList) {
            float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }
        int[][] icosahedronFaceList = new int[][] { { 3, 7, 1 }, { 4, 7, 3 }, { 6, 7, 4 }, { 8, 7, 6 }, { 7, 8, 1 }, { 9, 4, 3 }, { 2, 9, 3 }, { 2, 3, 1 }, { 11, 2, 1 }, { 10, 2, 11 }, { 10, 9, 2 }, { 9, 5, 4 }, { 6, 4, 5 }, { 0, 6, 5 }, { 0, 11, 8 }, { 11, 1, 8 }, { 10, 0, 5 }, { 10, 5, 9 }, { 0, 8, 6 }, { 0, 10, 11 }, };
        ArrayList<Float> vertices = new ArrayList<Float>();
        ArrayList<Integer> faceIndices = new ArrayList<Integer>();
        for (float[] v : icosahedronVertexList) {
            vertices.add(v[0]);
            vertices.add(v[2]);
            vertices.add(v[1]);
        }
        for (int[] f : icosahedronFaceList) {
            subdivide(f[0], f[1], f[2], vertices, faceIndices, level);
        }
        int beginPositionNumberCorona = vertices.size() / 3;
        float r = 40.f;
        vertices.add(-r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(r);
        vertices.add(0f);

        vertices.add(r);
        vertices.add(-r);
        vertices.add(0f);

        vertices.add(-r);
        vertices.add(-r);
        vertices.add(0f);

        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 0);
        faceIndices.add(beginPositionNumberCorona + 3);
        FloatBuffer positionBuffer = FloatBuffer.allocate(vertices.size());
        for (Float vert : vertices) {
            if (vert == 0f)
                vert = Math.nextAfter(vert, vert + 1.0f);
            positionBuffer.put(vert);

        }
        positionBuffer.flip();
        IntBuffer indexBuffer = IntBuffer.allocate(faceIndices.size());

        for (int i : faceIndices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();

        return new Pair<FloatBuffer, IntBuffer>(positionBuffer, indexBuffer);
    }

    private static void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
        if (level != 0) {
            float x1 = (vertexList.get(3 * vx) + vertexList.get(3 * vy));
            float y1 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vy + 1));
            float z1 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vy + 2));
            float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            x1 /= length;
            y1 /= length;
            z1 /= length;
            int firstIndex = vertexList.size() / 3;
            vertexList.add(x1);
            vertexList.add(y1);
            vertexList.add(z1);

            float x2 = (vertexList.get(3 * vz) + vertexList.get(3 * vy));
            float y2 = (vertexList.get(3 * vz + 1) + vertexList.get(3 * vy + 1));
            float z2 = (vertexList.get(3 * vz + 2) + vertexList.get(3 * vy + 2));
            length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            x2 /= length;
            y2 /= length;
            z2 /= length;
            int secondIndex = vertexList.size() / 3;
            vertexList.add(x2);
            vertexList.add(y2);
            vertexList.add(z2);

            float x3 = (vertexList.get(3 * vx) + vertexList.get(3 * vz));
            float y3 = (vertexList.get(3 * vx + 1) + vertexList.get(3 * vz + 1));
            float z3 = (vertexList.get(3 * vx + 2) + vertexList.get(3 * vz + 2));
            length = (float) Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
            x3 /= length;
            y3 /= length;
            z3 /= length;
            int thirdIndex = vertexList.size() / 3;
            vertexList.add(x3);
            vertexList.add(y3);
            vertexList.add(z3);

            subdivide(vx, firstIndex, thirdIndex, vertexList, faceList, level - 1);
            subdivide(firstIndex, vy, secondIndex, vertexList, faceList, level - 1);
            subdivide(thirdIndex, secondIndex, vz, vertexList, faceList, level - 1);
            subdivide(firstIndex, secondIndex, thirdIndex, vertexList, faceList, level - 1);
        } else {
            faceList.add(vx);
            faceList.add(vy);
            faceList.add(vz);
        }
    }

    public static void toggleCorona() {
        showCorona = !showCorona;
    }

    @Override
    public RenderableType getType() {
        return type;
    }

    @Override
    public Component getOptionsPanel() {
        FiltersPanel fp = ImageViewerGui.getFiltersPanel();
        fp.setActiveImage(glImage);
        return fp;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }

    @Override
    public String getName() {
        return mainLayerView.getName();
    }

    @Override
    public String getTimeString() {
        return mainLayerView.getMetaData().getDateObs().getCachedDate();
    }

    public AbstractView getMainLayerView() {
        return mainLayerView;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    @Override
    public boolean isActiveImageLayer() {
        return Layers.getActiveView() == mainLayerView;
    }

    @Override
    public void dispose(GL2 gl) {
        disablePositionVBO(gl);
        disableIndexVBO(gl);
        deletePositionVBO(gl);
        deleteIndexVBO(gl);
        glImage.dispose(gl);
    }

    private ImageData imageData;

    public void setImageData(ImageData _imageData) {
        imageData = _imageData;
    }

}
