package org.helioviewer.jhv.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.view.View;

/**
 * Extension of JSlider displaying the caching status on the track.
 *
 * This element provides its own look and feel. Therefore, it is independent
 * from the global look and feel.
 */
@SuppressWarnings("serial")
public class TimeSlider extends JSlider implements LazyComponent, MouseListener, MouseMotionListener, MouseWheelListener {

    private final TimeSliderUI ui;
    private boolean dirty;
    private boolean wasPlaying;

    public TimeSlider(int _orientation, int min, int max, int value) {
        super(_orientation, min, max, value);
        setSnapToTicks(true);

        ui = new TimeSliderUI(this);
        setUI(ui);
        // remove all mouse listeners installed by BasicSliderUI.TrackListener
        for (MouseListener l : getMouseListeners())
            removeMouseListener(l);
        for (MouseMotionListener l : getMouseMotionListeners())
            removeMouseMotionListener(l);
        for (MouseWheelListener l : getMouseWheelListeners())
            removeMouseWheelListener(l);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    // Overrides updateUI, to keep own SliderUI
    @Override
    public void updateUI() {
    }

    @Override
    public void repaint() {
        dirty = true;
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        dirty = true;
    }

    @Override
    public void lazyRepaint() {
        if (dirty) {
            super.repaint();
            dirty = false;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0)
            Layers.nextFrame();
        else if (e.getWheelRotation() > 0)
            Layers.previousFrame();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setValue(ui.valueForXPosition(e.getX()));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        wasPlaying = Layers.isMoviePlaying();
        if (wasPlaying)
            Layers.pauseMovie();
        mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (wasPlaying)
            Layers.playMovie();
    }

    /**
     * Extension of BasicSliderUI overriding some drawing functions.
     * All functions for size calculations stay the same.
     */
    private static class TimeSliderUI extends BasicSliderUI {

        private static final Color notCachedColor = Color.LIGHT_GRAY;
        private static final Color partialCachedColor = Color.GRAY;
        private static final Color completeCachedColor = Color.BLACK;

        private static final BasicStroke thickStroke = new BasicStroke(4);

        public TimeSliderUI(JSlider component) {
            super(component);
        }

        @Override
        public void paintThumb(Graphics g) {
            g.setColor(Color.BLACK);
            g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);

            int x = thumbRect.x + (thumbRect.width - 1) / 2;
            g.drawLine(x, thumbRect.y, x, thumbRect.y + thumbRect.height - 1);
        }

        // Draws the different regions: no/partial/complete information
        @Override
        public void paintTrack(Graphics g1) {
            Graphics2D g = (Graphics2D) g1.create();
            g.setStroke(thickStroke);

            int y = slider.getSize().height / 2;
            View view = Layers.getActiveView();
            if (view == null) {
                g.setColor(notCachedColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else if (view.isComplete()){
                g.setColor(completeCachedColor);
                g.drawLine(trackRect.x, y, trackRect.x + trackRect.width, y);
            } else {
                int len = view.getMaximumFrameNumber();
                for (int i = 0; i < len; i++) {
                    int begin = (int) ((float) i / len * trackRect.width);
                    int end = (int) ((float) (i + 1) / len * trackRect.width);

                    if (end == begin)
                        end++;

                    AtomicBoolean status = view.getFrameCacheStatus(i);
                    if (status == null)
                        g.setColor(notCachedColor);
                    else {
                        boolean complete = status.get();
                        if (complete)
                            g.setColor(completeCachedColor);
                        else
                            g.setColor(partialCachedColor);
                    }
                    g.drawLine(trackRect.x + begin, y, trackRect.x + end, y);
                }
            }
            g.dispose();
        }

    }

}
