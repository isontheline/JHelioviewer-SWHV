package org.helioviewer.jhv.camera.object;

import org.helioviewer.jhv.astronomy.SpaceObject;

public class SpaceObjectElement {

    private final SpaceObject object;

    private boolean selected;
    private String status;

    public SpaceObjectElement(SpaceObject _object) {
        object = _object;
    }

    public SpaceObject getObject() {
        return object;
    }

    public void select(String frame, long startTime, long endTime) {
        selected = true;
    }

    public void deselect() {
        selected = false;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setStatus(String _status) {
        status = _status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return object.toString();
    }

}