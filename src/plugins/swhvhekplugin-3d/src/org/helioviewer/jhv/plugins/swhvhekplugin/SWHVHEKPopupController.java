package org.helioviewer.jhv.plugins.swhvhekplugin;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.controller.InputControllerPlugin;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.opengl.GLHelper;

public class SWHVHEKPopupController implements MouseListener, MouseMotionListener, InputControllerPlugin, TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private static Component component;
    private static Camera camera;

    protected static JHVRelatedEvents mouseOverJHVEvent = null;
    protected static Point mouseOverPosition = null;
    private static Cursor lastCursor;

    protected Date currentTime;

    @Override
    public void setComponent(Component _component) {
        component = _component;
    }

    @Override
    public void setCamera(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void timeChanged(JHVDate date) {
        currentTime = date.getDate();
    }

    private Point calcWindowPosition(Point p, int hekWidth, int hekHeight) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;

        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        int compLocX = component.getLocationOnScreen().x;
        int compLocY = component.getLocationOnScreen().y;

        if (p.y + hekHeight + yOffset < compHeight) {
            yCoord = p.y + compLocY + yOffset;
        } else {
            yCoord = p.y + compLocY - hekHeight - yOffset;
            if (yCoord < compLocY) {
                yCoord = compLocY + compHeight - hekHeight;
                if (yCoord < compLocY) {
                    yCoord = compLocY;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord = 0;
        if (p.x + hekWidth + xOffset < compWidth) {
            xCoord = p.x + compLocX + xOffset;
        } else {
            xCoord = p.x + compLocX - hekWidth - xOffset;
            if (xCoord < compLocX && !yCoordInMiddle) {
                xCoord = compLocX + compWidth - hekWidth;
            }
        }

        return new Point(xCoord, yCoord);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (mouseOverJHVEvent != null) {
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent, mouseOverJHVEvent.getClosestTo(currentTime));
            hekPopUp.setLocation(calcWindowPosition(GLHelper.GL2AWTPoint(mouseOverPosition.x, mouseOverPosition.y), hekPopUp.getWidth(), hekPopUp.getHeight()));
            hekPopUp.pack();
            hekPopUp.setVisible(true);

            component.setCursor(helpCursor);
        }

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverPosition = null;
        mouseOverJHVEvent = null;
        JHVEventContainer.highlight(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ArrayList<JHVRelatedEvents> eventsToDraw = SWHVHEKData.getSingletonInstance().getActiveEvents(currentTime);
        if (eventsToDraw.size() == 0)
            return;

        mouseOverJHVEvent = null;
        mouseOverPosition = null;
        Vec3 pt = null;
        Vec3 hitpoint = null;

        Viewport vp = Displayer.getActiveViewport();
        for (JHVRelatedEvents evtr : eventsToDraw) {
            JHVEvent evt = evtr.getClosestTo(this.currentTime);
            JHVPositionInformation pi = evt.getPositioningInformation();
            if (Displayer.mode == Displayer.DisplayMode.ORTHO) {
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    Map<String, JHVEventParameter> params = evt.getAllEventParameters();
                    double principalAngle = Math.toRadians(SWHVHEKData.readCMEPrincipalAngleDegree(params));
                    double speed = SWHVHEKData.readCMESpeed(params);
                    double distSun = 2.4;
                    distSun += speed * (currentTime.getTime() - evt.getStartDate().getTime()) / Sun.RadiusMeter;

                    Position.Q p = evt.getEarthPosition();
                    hitpoint = p.orientation.rotateInverseVector(getHitPointPlane(e, vp));
                    pt = p.orientation.rotateInverseVector(new Vec3(distSun * Math.cos(principalAngle), distSun * Math.sin(principalAngle), 0));
                } else {
                    hitpoint = getHitPoint(e, vp);
                    pt = pi.centralPoint();
                }

                if (pt != null && hitpoint != null) {
                    double deltaX = Math.abs(hitpoint.x - pt.x);
                    double deltaY = Math.abs(hitpoint.y - pt.y);
                    double deltaZ = Math.abs(hitpoint.z - pt.z);
                    if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                        mouseOverJHVEvent = evtr;
                        mouseOverPosition = e.getPoint();
                        break;
                    }
                }
            }
            else {
                Vec2 tf = null;
                Vec2 mousepos = null;
                if (evt.getName().equals("Coronal Mass Ejection")) {
                    if (Displayer.mode == Displayer.DisplayMode.LOGPOLAR || Displayer.mode == Displayer.DisplayMode.POLAR) {
                        Map<String, JHVEventParameter> params = evt.getAllEventParameters();
                        double principalAngle = SWHVHEKData.readCMEPrincipalAngleDegree(params) - 90;
                        double speed = SWHVHEKData.readCMESpeed(params);
                        double distSun = 2.4;
                        distSun += speed * (currentTime.getTime() - evt.getStartDate().getTime()) / Sun.RadiusMeter;
                        GridScale scale = GridScale.current;
                        tf = new Vec2(scale.getXValueInv(principalAngle), scale.getYValueInv(distSun));
                        mousepos = scale.mouseToGridInv(e.getPoint(), vp, camera);
                    }
                } else {
                    hitpoint = getHitPoint(e, vp);
                    pt = pi.centralPoint();
                    pt = camera.getViewpoint().orientation.rotateVector(pt);
                    GridScale scale = GridScale.current;
                    tf = scale.transform(pt);
                    mousepos = scale.mouseToGridInv(e.getPoint(), vp, camera);
                }

                if (tf != null && mousepos != null) {
                    double deltaX = Math.abs(tf.x - mousepos.x);
                    double deltaY = Math.abs(tf.y - mousepos.y);
                    if (deltaX < 0.02 && deltaY < 0.02) {
                        mouseOverJHVEvent = evtr;
                        mouseOverPosition = e.getPoint();
                        break;
                    }
                }
            }
        }

        JHVEventContainer.highlight(mouseOverJHVEvent);
        if (helpCursor != component.getCursor())
            lastCursor = component.getCursor();

        if (mouseOverJHVEvent != null) {
            component.setCursor(helpCursor);
        } else {
            component.setCursor(lastCursor);
        }
    }

    private Vec3 getHitPointPlane(MouseEvent e, Viewport vp) {
        return CameraHelper.getVectorFromPlane(camera, vp, e.getPoint());
    }

    private Vec3 getHitPoint(MouseEvent e, Viewport vp) {
        Vec3 hp = CameraHelper.getVectorFromSphere(camera, vp, e.getPoint());
        if (hp != null)
            hp.y = -hp.y;
        return hp;
    }

}
