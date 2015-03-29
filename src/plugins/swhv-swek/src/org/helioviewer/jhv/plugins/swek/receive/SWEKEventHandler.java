package org.helioviewer.jhv.plugins.swek.receive;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;

public class SWEKEventHandler implements JHVEventHandler {

    private static final SWEKEventHandler instance = new SWEKEventHandler();

    /** the listeners */
    private static final ArrayList<SWEKEventHandlerListener> listeners = new ArrayList<SWEKEventHandlerListener>();

    /**
     * Private default constructor
     */
    private SWEKEventHandler() {
    }

    /**
     * Gets the singleton instance of the SWEKEventHandler
     *
     * @return the singleton instance
     */
    public static SWEKEventHandler getSingletonInstance() {
        return instance;
    }

    /**
     * Adds a SWEKEventHandlerListener to the SWEKEventHandler
     *
     * @param listener
     *            the listener to add
     */
    public void addSWEKEventHandlerListener(SWEKEventHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the SWEKEventHandlerListener from the SWEKEventHandler
     *
     * @param listener
     *            the listener to remove
     */
    public void removeSWEKEventHandkerListener(SWEKEventHandlerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void newEventsReceived(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> eventList) {
    }

    @Override
    public void cacheUpdated() {
    }

}
