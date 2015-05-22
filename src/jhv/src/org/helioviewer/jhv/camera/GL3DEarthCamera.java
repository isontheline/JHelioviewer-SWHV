package org.helioviewer.jhv.camera;

import java.util.Date;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

public class GL3DEarthCamera extends GL3DCamera {

    private final GL3DEarthCameraOptionPanel earthCameraOptionPanel;

    public GL3DEarthCamera() {
        super();
        earthCameraOptionPanel = new GL3DEarthCameraOptionPanel(this);
    }

    @Override
    public void reset() {
        this.resetCurrentDragRotation();
        super.reset();
        this.forceTimeChanged(Displayer.getLastUpdatedTimestamp());
    }

    @Override
    public void activate(GL3DCamera precedingCamera) {
        super.activate(precedingCamera);
        this.timeChanged(Displayer.getLastUpdatedTimestamp());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getName() {
        return "View from Earth";
    }

    @Override
    public void timeChanged(Date date) {
        if (!this.getTrackingMode()) {
            forceTimeChanged(date);
        }
    }

    public void forceTimeChanged(Date date) {
        if (date != null) {
            updateRotation(date);

            RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
            if (renderableCamera != null) {
                renderableCamera.setTimeString(date);
                ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
            }
        }
    }

    private void updateRotation(Date date) {
        double b = Astronomy.getB0Radians(date);
        double l = Astronomy.getL0Radians(date);
        double d = Astronomy.getDistanceSolarRadii(date);

        this.localRotation = new GL3DQuatd(b, l);
        this.setZTranslation(-d);

        this.updateCameraTransformation();
    }

    @Override
    public GL3DCameraOptionPanel getOptionPanel() {
        return earthCameraOptionPanel;
    }

}
