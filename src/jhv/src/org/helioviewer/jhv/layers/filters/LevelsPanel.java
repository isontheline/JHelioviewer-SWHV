package org.helioviewer.jhv.layers.filters;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.layers.ImageLayerOptions;

import com.jidesoft.swing.RangeSlider;

public class LevelsPanel implements ChangeListener, FilterDetails {

    private final RangeSlider slider;
    private final JLabel label;
    private final JPanel buttonPanel;

    static String align3(int value) {
        if (value < -99)
            return value + "%";
        if (value < -9)
            return "\u2007" + value + "%";
        if (value < 0)
            return "\u2007\u2007" + value + "%";
        if (value < 10)
            return "\u2007\u2007\u2007" + value + "%";
        if (value < 100)
            return "\u2007\u2007" + value + "%";
        return "\u2007" + value + "%";
    }

    private String format(int low, int high) {
        return "<html>" + align3(low) + "<br>" + align3(high);
    }

    public LevelsPanel() {
        slider = new RangeSlider(-100, 200, 0, 100);
        slider.setRangeDraggable(true);
        slider.addChangeListener(this);
//        WheelSupport.installMouseWheelSupport(slider);

        label = new JLabel(format(slider.getLowValue(), slider.getHighValue()), JLabel.RIGHT);
        label.setMinimumSize(new JLabel(format(-100, -100)).getPreferredSize());

        JButton autoButton = new JButton("Auto");
        autoButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        autoButton.setBorderPainted(false);
        autoButton.setFocusPainted(false);
        autoButton.setContentAreaFilled(false);
        autoButton.setToolTipText("Auto brightness");
        autoButton.addActionListener(e -> {
            double auto = ((ImageLayerOptions) getComponent().getParent()).getAutoBrightness();
            slider.setHighValue((int) (auto * 100));
        });

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(label, BorderLayout.LINE_START);
        buttonPanel.add(autoButton, BorderLayout.LINE_END);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        ((ImageLayerOptions) getComponent().getParent()).getGLImage().setLevels(slider.getLowValue() / 100f, slider.getHighValue() / 100f);
        label.setText(format(slider.getLowValue(), slider.getHighValue()));
        Displayer.display();
    }

    @Override
    public Component getTitle() {
        return new JLabel("Levels", JLabel.RIGHT);
    }

    @Override
    public Component getComponent() {
        return slider;
    }

    @Override
    public Component getLabel() {
        return buttonPanel;
    }

}