package org.helioviewer.jhv.renderable.viewport;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

public class GL3DViewport {

    private int w;
    private int h;
    private int x;
    private int y;
    private boolean isVisible = true;
    private GL3DCamera camera;
    private final boolean movieExport;

    public GL3DViewport(int _x, int _y, int _w, int _h, GL3DCamera _camera) {
        this(_x, _y, _w, _h, _camera, false);

    }

    public GL3DViewport(int _x, int _y, int _w, int _h, GL3DCamera _camera, boolean _movieExport) {
        w = _w;
        h = _h;
        x = _x;
        y = _y;
        camera = _camera;
        movieExport = _movieExport;
    }

    public GL3DCamera getCamera() {
        return camera;
    }

    public void setCamera(GL3DCamera _camera) {
        camera = _camera;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int getOffsetX() {
        if (movieExport)
            return 0;
        return x;
    }

    public int getOffsetY() {
        if (movieExport)
            return 0;
        return Displayer.getViewport().getHeight() - h - y;
    }

    public void setViewportSize(int width, int height) {
        w = width;
        h = height;
    }

    public void setViewportOffset(int offsetX, int offsetY) {
        x = offsetX;
        y = offsetY;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }

    @Override
    public String toString() {
        return "Offset: " + this.getOffsetX() + "," + this.getOffsetY() + " Size: " + this.getWidth() + "," + this.getHeight();
    }
}
