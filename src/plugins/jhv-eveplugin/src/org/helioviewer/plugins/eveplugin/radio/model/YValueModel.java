/**
 * 
 */
package org.helioviewer.plugins.eveplugin.radio.model;

import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;

/**
 * Keeps the y value information for one plot identifier.
 * 
 * @author Bram.Bourgoignie@oma.be
 */
public class YValueModel implements PlotAreaSpaceListener {

    /** The available minimum y-value. */
    private double availableYMin;

    /** The available maximum y-value. */
    private double availableYMax;

    /** The selected minimum y-value. */
    private double selectedYMin;

    /** The selected maximum y-value. */
    private double selectedYMax;

    /** The plot area space corresponding with the y-value model */
    private final PlotAreaSpace pas;

    /**
     * Constructor
     */
    public YValueModel(String plotIdentifier) {
        pas = PlotAreaSpaceManager.getInstance().getPlotAreaSpace(plotIdentifier);
    }

    /**
     * Gives the available minimum y-value.
     * 
     * @return The available minimum y-value
     */
    public synchronized double getAvailableYMin() {
        return availableYMin;
    }

    /**
     * Sets the available minimum y-value.
     * 
     * @param availableYMin
     *            The new available minimum y-value
     */
    public synchronized void setAvailableYMin(double availableYMin) {
        this.availableYMin = availableYMin;
        recalculateSelectedInterval();
    }

    /**
     * Gives the available maximum y-value.
     * 
     * @return The new available maximum y-value
     */
    public synchronized double getAvailableYMax() {
        return availableYMax;
    }

    /**
     * Sets the available maximum y-value
     * 
     * @param availableYMax
     */
    public synchronized void setAvailableYMax(double availableYMax) {
        this.availableYMax = availableYMax;
        recalculateSelectedInterval();
    }

    /**
     * Gets the selected minimum y-value.
     * 
     * @return The selected minimum y-value
     */
    public synchronized double getSelectedYMin() {
        return selectedYMin;
    }

    /**
     * Sets the selected minimum y-value.
     * 
     * @param selectedYMin
     *            The new selected minimum y-value
     */
    public synchronized void setSelectedYMin(double selectedYMin) {
        this.selectedYMin = selectedYMin;
    }

    /**
     * Gets the selected maximum y-value.
     * 
     * @return The selected maximum y-value
     */
    public synchronized double getSelectedYMax() {
        return selectedYMax;
    }

    /**
     * Sets the selected maximum y-value.
     * 
     * @param selectedYMax
     *            The new selected maximum y-value
     */
    public void setSelectedYMax(double selectedYMax) {
        this.selectedYMax = selectedYMax;
    }

    /*
     * PlotAreaSpaceListener
     */
    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
        double scaledDiff = scaledMaxValue - scaledMinValue;
        double absDiff = availableYMax - availableYMin;
        double freqPerScaled = absDiff / scaledDiff;
        selectedYMin = (1.0 * availableYMin + (scaledMaxValue - scaledSelectedMaxValue) * freqPerScaled);
        selectedYMax = (1.0 * availableYMin + (scaledMaxValue - scaledSelectedMinValue) * freqPerScaled);
    }

    /**
     * Recalculates the selected interval based on the plot area space
     * corresponding with this y-value model.
     */
    private void recalculateSelectedInterval() {
        double scaledDiff = pas.getScaledMaxValue() - pas.getScaledMinValue();
        double absDiff = availableYMax - availableYMin;
        double freqPerScaled = absDiff / scaledDiff;
        selectedYMin = (1.0 * availableYMin + (pas.getScaledSelectedMinValue() - pas.getScaledMinValue()) * freqPerScaled);
        selectedYMax = (1.0 * availableYMin + (pas.getScaledSelectedMaxValue() - pas.getScaledMinValue()) * freqPerScaled);
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime,
            double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub

    }

}
