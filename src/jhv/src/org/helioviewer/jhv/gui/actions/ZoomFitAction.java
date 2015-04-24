package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Action to zoom such that the active layer fits completely in the viewport.
 */
public class ZoomFitAction extends AbstractAction {

    /**
     * Constructor
     *
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomFitAction(boolean small) {
        super("Zoom to fit", small ? IconBank.getIcon(JHVIcon.ZOOM_FIT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_FIT));
        putValue(SHORT_DESCRIPTION, "Zoom to fit");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_MASK));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractView view = Displayer.getLayersModel().getActiveView();
        GL3DCamera camera = Displayer.getActiveCamera();
        System.out.println(view);
        if (view != null) {
            Region region = view.getMetaData().getPhysicalRegion();
            double imheight = ((HelioviewerMetaData) view.getMetaData()).pixelImageHeight();
            double imageFraction = Displayer.getViewportHeight() / imheight;
            if (region != null) {
                double fov = 2. * Math.atan(-region.getHeight() * imageFraction / 2. / camera.getTranslation().z);
                camera.setCameraFOV(fov);
                Displayer.display();
            }
        }
    }

}
