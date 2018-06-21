package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

import com.jogamp.newt.event.MouseEvent;

public class InteractionAxis extends Interaction {

    private Vec3 currentRotationStartPoint;

    public InteractionAxis(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), Sun.Radius2);
        double len2 = currentRotationStartPoint.length2();
        if (len2 > Sun.Radius2) {
            double w = camera.getWidth();
            currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), w * w);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRotationStartPoint == null) // freak crash
            return;

        double len2 = currentRotationStartPoint.length2();
        Vec3 currentRotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), len2);

        Vec3 axis = camera.getUpdateViewpoint() == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint).twist(axis));
        Display.display();
    }

}
