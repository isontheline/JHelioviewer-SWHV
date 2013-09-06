package org.helioviewer.plugins.eveplugin.model;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.EVEValues;

/**
 * 
 * @author Stephan Pagel
 * */
public class EVEBandCache {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private final HashMap<Band, EVECache> cacheMap = new HashMap<Band, EVECache>();
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public void add(final Band band, final EVEValue[] values) {
        EVECache cache = cacheMap.get(band);
        
        if (cache == null) {
            cache = new EVECache();
            cacheMap.put(band, cache);
        }
        
        cache.add(values);
    }
    
    public EVEValues getValuesInInterval(final Band band, final Interval<Date> interval) {
        EVECache cache = cacheMap.get(band);
        
        if (cache == null) {
            return new EVEValues();
        }
        
        return cache.getValuesInInterval(interval, band.getBandType().getErrorLevels());
    }
    
    public Range getMinMaxInInterval(final Band band, final Interval<Date> interval) {
        final EVECache cache = cacheMap.get(band);
        
        if (cache == null) {
            return new Range();
        }
        
        return cache.getMinMaxInInterval(interval);
    }
    
    public List<Interval<Date>> getMissingDatesInInterval(final Band band, final Interval<Date> interval) {
        EVECache cache = cacheMap.get(band);
        
        if (cache == null) {
            List<Interval<Date>> list = new LinkedList<Interval<Date>>();
            list.add(interval);
            return list;
        }
        
        return cache.getMissingDatesInInterval(interval);
    }
}
