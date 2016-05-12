package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
class LineOptionPanel extends SmallPanel {

    private final Band band;

    public LineOptionPanel(Band _band) {
        band = _band;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        JButton pickColor = new JButton("Line color");
        pickColor.setMargin(new Insets(0, 0, 0, 0));
        pickColor.setToolTipText("Change the color of the current line");
        pickColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(ImageViewerGui.getMainFrame(), "Choose Line Color", band.getDataColor());
                if (newColor != null) {
                    band.setDataColor(newColor);
                }
            }
        });
        add(pickColor, c);

        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        JButton availabilityButton = new JButton("Available data");
        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = Settings.getSingletonInstance().getProperty("availability.timelines.url");
                url += "#" + band.getBandType().getName();
                JHVGlobals.openURL(url);
            }
        });
        add(availabilityButton, c);

        setSmall();
    }

}