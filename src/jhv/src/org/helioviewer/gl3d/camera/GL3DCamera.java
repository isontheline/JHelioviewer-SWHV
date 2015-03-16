package org.helioviewer.gl3d.camera;

import java.awt.Point;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.math.GL3DVec4d;
import org.helioviewer.gl3d.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DGrid;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;

/**
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DCamera {

    protected GLU glu = new GLU();

    public static final double MAX_DISTANCE = -Constants.SunMeanDistanceToEarth * 1.8;
    public static final double MIN_DISTANCE = -Constants.SunRadius * 1.2;
    public static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    public static final double MIN_FOV = INITFOV * 0.05;
    public static final double MAX_FOV = INITFOV * 100;
    private final double clipNear = Constants.SunRadius * 3;
    private final double clipFar = Constants.SunRadius * 10000.;
    private double fov = INITFOV;
    private double aspect = 0.0;

    private GL3DMat4d cameraTransformation;

    protected GL3DQuatd rotation;
    protected GL3DVec3d translation;

    protected GL3DQuatd currentDragRotation;

    protected GL3DQuatd localRotation;

    private long timeDelay;

    private double translationz;

    private double ratio = 1.0;

    private double gridResolutionX = 20;
    private double gridResolutionY = 20;
    protected GL3DGrid grid;
    private GL3DGrid followGrid;

    private long time;

    private boolean trackingMode;

    protected GL3DSceneGraphView sceneGraphView;

    private GL3DMat4d orthoMatrix = GL3DMat4d.identity();

    public GL3DCamera(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = new GL3DQuatd();
        this.currentDragRotation = new GL3DQuatd();
        this.localRotation = new GL3DQuatd();
        this.translation = new GL3DVec3d();
        GL3DGrid grid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        GL3DGrid followGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);
        this.sceneGraphView.getRoot().addNode(grid);
        this.sceneGraphView.getRoot().addNode(followGrid);
        this.grid = grid;
        this.followGrid = followGrid;
        this.grid.getDrawBits().on(Bit.Hidden);
        this.followGrid.getDrawBits().on(Bit.Hidden);
    }

    public GL3DGrid getGrid() {
        return this.grid;
    }

    public GL3DGrid setFollowGrid(GL3DGrid followgrid) {
        return this.followGrid = followgrid;

    }

    public void setGridResolution(int resolution) {
        this.setGridResolutionX(resolution);
        this.setGridResolutionY(resolution);
        createNewGrid();
    }

    public void setGridResolutionX(double resolution) {
        this.gridResolutionX = resolution;
        createNewGrid();
    }

    public void setGridResolutionY(double resolution) {
        this.gridResolutionY = resolution;
        createNewGrid();
    }

    protected GL3DSceneGraphView getSceneGraphView() {
        return this.sceneGraphView;
    }

    public void reset() {
        this.resetFOV();
        this.translation = new GL3DVec3d(0, 0, this.translation.z);
    }

    private void resetFOV() {
        this.fov = INITFOV;
    }

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     *
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            this.rotation = precedingCamera.getRotation().copy();
            this.translation = precedingCamera.translation.copy();
            this.updateCameraTransformation();

            // Also set the correct interaction
            if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getRotateInteraction())) {
                this.setCurrentInteraction(this.getRotateInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getPanInteraction())) {
                this.setCurrentInteraction(this.getPanInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getZoomInteraction())) {
                this.setCurrentInteraction(this.getZoomInteraction());
            }
        } else {
            Log.debug("GL3DCamera: No Preceding Camera, resetting Camera");
            this.reset();
        }
    }

    protected void setZTranslation(double z) {
        this.translationz = Math.min(MIN_DISTANCE, Math.max(MAX_DISTANCE, z));
        this.translation.z = this.ratio * this.translationz;
    }

    protected void addPanning(double x, double y) {
        setPanning(this.translation.x + x, this.translation.y + y);
    }

    public void setPanning(double x, double y) {
        this.translation.x = x;
        this.translation.y = y;
    }

    public GL3DVec3d getTranslation() {
        return this.translation;
    }

    public GL3DMat4d getCameraTransformation() {
        return this.cameraTransformation;
    }

    public double getZTranslation() {
        return this.translation.z;
    }

    public GL3DQuatd getLocalRotation() {
        return this.localRotation;
    }

    public GL3DQuatd getRotation() {
        this.updateCameraTransformation();
        return this.rotation;
    }

    public void resetCurrentDragRotation() {
        this.currentDragRotation.clear();
    }

    public void setLocalRotation(GL3DQuatd localRotation) {
        this.localRotation = localRotation;
        this.rotation.clear();
        this.updateCameraTransformation();

    }

    public void setCurrentDragRotation(GL3DQuatd currentDragRotation) {
        this.currentDragRotation = currentDragRotation;
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    public void rotateCurrentDragRotation(GL3DQuatd currentDragRotation) {
        this.currentDragRotation.rotate(currentDragRotation);
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    public void setSceneGraphView(GL3DSceneGraphView sgv) {
        this.sceneGraphView = sgv;
    }

    public void activate() {
        //this.getGrid().getDrawBits().off(Bit.Hidden);
    }

    public void createNewGrid() {
        this.createNewGrid(this.sceneGraphView);
    }

    public void createNewGrid(GL3DSceneGraphView gv) {
        boolean hidden = getGrid().getDrawBits().get(Bit.Hidden);
        this.sceneGraphView.getRoot().removeNode(this.grid);
        this.sceneGraphView.getRoot().removeNode(this.followGrid);

        GL3DGrid newGrid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        newGrid.getDrawBits().set(Bit.Hidden, hidden);
        GL3DGrid followCameraGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);
        followCameraGrid.getDrawBits().set(Bit.Hidden, hidden);
        gv.getRoot().addNode(this.grid);
        gv.getRoot().addNode(this.followGrid);
    }

    public void applyPerspective(GL3DState state) {
        GL2 gl = state.gl;

        this.aspect = state.getViewportWidth() / (double) state.getViewportHeight();

        gl.glMatrixMode(GL2.GL_PROJECTION);

        gl.glPushMatrix();
        gl.glLoadIdentity();
        double w = -translation.z * Math.tan(fov / 2.);
        gl.glOrtho(-this.aspect * w, this.aspect * w, -w, w, this.clipNear, this.clipFar);
        this.orthoMatrix = GL3DMat4d.ortho(-w, w, -w, w, this.clipNear, this.clipFar);
        this.orthoMatrix.translate(new GL3DVec3d(this.translation.x, this.translation.y, 0.));
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public GL3DVec3d getVectorFromSphere(Point viewportCoordinates) {
        GL3DState state = GL3DState.get();
        /* workaround for null GL3DState on startup */
        if (state == null) {
            return null;
        }

        GL3DMat4d vpmi = this.orthoMatrix.inverse();

        double aspect = state.getViewportWidth() / (double) state.getViewportHeight();
        GL3DVec4d centeredViewportCoordinates = new GL3DVec4d(GLInfo.pixelScale[0] * (2. * viewportCoordinates.getX() / state.getViewportWidth() - 0.5) * aspect, -GLInfo.pixelScale[1] * (2. * viewportCoordinates.getY() / state.getViewportHeight() - 0.5), 0., 0.);

        GL3DVec4d solarCoordinates = vpmi.multiply(centeredViewportCoordinates);
        solarCoordinates.w = 0.;
        GL3DMat4d roti = this.getRotation().toMatrix().inverse();

        double solarCoordinates3Dz = Math.sqrt(1 - solarCoordinates.dot(solarCoordinates));

        solarCoordinates.z = solarCoordinates3Dz;
        GL3DVec4d notRotated = roti.multiply(solarCoordinates);

        if (Double.isNaN(notRotated.z)) {
            return null;
        }
        GL3DVec3d solarCoordinates3D = new GL3DVec3d(notRotated.x, notRotated.y, notRotated.z);

        return solarCoordinates3D;
    }

    public GL3DVec3d getVectorFromSphereAlt(Point viewportCoordinates) {
        GL3DState state = GL3DState.get();
        /* workaround for null GL3DState on startup */
        if (state == null) {
            return null;
        }

        GL3DMat4d vpmi = this.orthoMatrix.inverse();
        GL3DMat4d tli = GL3DMat4d.identity();

        double aspect = state.getViewportWidth() / (double) state.getViewportHeight();
        GL3DVec4d centeredViewportCoordinates = new GL3DVec4d(GLInfo.pixelScale[0] * (2. * viewportCoordinates.getX() / state.getViewportWidth() - 0.5) * aspect, -GLInfo.pixelScale[1] * (2. * viewportCoordinates.getY() / state.getViewportHeight() - 0.5), 0., 0.);

        GL3DVec4d solarCoordinates = vpmi.multiply(centeredViewportCoordinates);
        solarCoordinates.w = 1.;
        solarCoordinates = tli.multiply(solarCoordinates);
        solarCoordinates.w = 0.;
        GL3DMat4d roti = this.getCurrentDragRotation().toMatrix().inverse();

        double solarCoordinates3Dz = Math.sqrt(1 - solarCoordinates.dot(solarCoordinates));

        solarCoordinates.z = solarCoordinates3Dz;
        if (Double.isNaN(solarCoordinates.z)) {
            solarCoordinates.z = 0;
        }
        GL3DVec4d notRotated = roti.multiply(solarCoordinates);

        if (Double.isNaN(notRotated.z)) {
            return null;
        }
        GL3DVec3d solarCoordinates3D = new GL3DVec3d(notRotated.x, notRotated.y, notRotated.z);

        return solarCoordinates3D;
    }

    public GL3DVec3d getVectorFromPlane(Point viewportCoordinates) {
        GL3DState state = GL3DState.get();
        GL3DMat4d roti = this.getRotation().toMatrix().inverse();
        GL3DMat4d vpmi = this.orthoMatrix.inverse();

        double aspect = state.getViewportWidth() / (double) state.getViewportHeight();
        GL3DVec4d centeredViewportCoordinates1 = new GL3DVec4d(GLInfo.pixelScale[0] * (2. * viewportCoordinates.getX() / state.getViewportWidth() - 0.5) * aspect, -GLInfo.pixelScale[1] * (2. * viewportCoordinates.getY() / state.getViewportHeight() - 0.5), -1., 1.);
        GL3DVec4d centeredViewportCoordinates2 = new GL3DVec4d(GLInfo.pixelScale[0] * (2. * viewportCoordinates.getX() / state.getViewportWidth() - 0.5) * aspect, -GLInfo.pixelScale[1] * (2. * viewportCoordinates.getY() / state.getViewportHeight() - 0.5), 1., 1.);

        GL3DVec4d up1 = roti.multiply(vpmi.multiply(centeredViewportCoordinates1));
        GL3DVec4d up2 = roti.multiply(vpmi.multiply(centeredViewportCoordinates2));
        GL3DVec4d linevec = GL3DVec4d.subtract(up2, up1);
        GL3DVec4d normal = this.getLocalRotation().toMatrix().inverse().multiply(new GL3DVec4d(0., 0., 1., 1.));
        double fact = -GL3DVec4d.dot3d(up1, normal) / GL3DVec4d.dot3d(linevec, normal);
        GL3DVec4d notRotated = GL3DVec4d.add(up1, GL3DVec4d.multiply(linevec, fact));

        GL3DVec3d solarCoordinates3D = new GL3DVec3d(notRotated.x, notRotated.y, notRotated.z);

        return solarCoordinates3D;
    }

    public void resumePerspective(GL3DState state) {
        GL2 gl = state.gl;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void updateCameraTransformation() {
        this.updateCameraTransformation(true);
    }

    public void updateCameraTransformation(GL3DMat4d transformation) {
        this.cameraTransformation = transformation;
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    public void updateCameraTransformation(boolean fireEvent) {
        this.rotation = this.currentDragRotation.copy();
        this.rotation.rotate(this.localRotation);

        cameraTransformation = GL3DMat4d.translation(this.translation);
        cameraTransformation.multiply(this.rotation.toMatrix());
    }

    public void applyCamera(GL3DState state) {
        state.multiplyMV(cameraTransformation);
    }

    public abstract GL3DMat4d getVM();

    public abstract double getDistanceToSunSurface();

    public abstract GL3DInteraction getPanInteraction();

    public abstract GL3DInteraction getRotateInteraction();

    public abstract GL3DInteraction getZoomInteraction();

    public abstract String getName();

    public void drawCamera(GL3DState state) {
        getCurrentInteraction().drawInteractionFeedback(state, this);
    }

    public abstract GL3DInteraction getCurrentInteraction();

    public abstract void setCurrentInteraction(GL3DInteraction currentInteraction);

    public double getCameraFOV() {
        return this.fov;
    }

    public double setCameraFOV(double fov) {
        if (fov < MIN_FOV) {
            this.fov = MIN_FOV;
        } else if (fov > MAX_FOV) {
            this.fov = MAX_FOV;
        } else {
            this.fov = fov;
        }
        return this.fov;
    }

    public final double getClipNear() {
        return clipNear;
    }

    public final double getClipFar() {
        return clipFar;
    }

    public double getAspect() {
        return aspect;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void updateRotation(long dateMillis) {

    }

    public void setTimeDelay(long timeDelay) {
        this.timeDelay = timeDelay;
    }

    public long getTimeDelay() {
        return this.timeDelay;
    }

    public GL3DQuatd getCurrentDragRotation() {
        return this.currentDragRotation;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
        this.translation.z = this.translationz * this.ratio;
    }

    public double getGridResolutionX() {
        return gridResolutionX;
    }

    public double getGridResolutionY() {
        return gridResolutionY;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTrackingMode(boolean trackingMode) {
        this.trackingMode = trackingMode;
    }

    public boolean getTrackingMode() {
        return this.trackingMode;
    }

    public void deactivate() {
    }

    public void setDefaultFOV() {
        this.fov = INITFOV;
    }

    public GL3DNode getFollowGrid() {
        return this.followGrid;
    }

}
