package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.database.EventDatabase;

class SWEKDownloadWorker implements Runnable {

    private final SWEKSupplier supplier;
    private final List<SWEKParam> params;
    private final Interval requestInterval;

    SWEKDownloadWorker(SWEKSupplier _supplier, Interval _interval, List<SWEKParam> _params) {
        requestInterval = _interval;
        supplier = _supplier;
        params = _params;
    }

    void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        SWEKSource swekSource = supplier.getSource();
        boolean success = swekSource.getHandler().remote2db(supplier, requestInterval.start, requestInterval.end, params);
        if (success) {
            ArrayList<JHVAssociation> assocList = EventDatabase.associations2Program(requestInterval.start, requestInterval.end, supplier);
            ArrayList<JHVEvent> eventList = EventDatabase.events2Program(requestInterval.start, requestInterval.end, supplier, params);
            EventQueue.invokeLater(() -> {
                assocList.forEach(JHVEventCache::add);
                eventList.forEach(JHVEventCache::add);

                JHVEventCache.fireEventCacheChanged();
                SWEKDownloadManager.workerFinished(this);
            });
            EventDatabase.addDaterange2db(requestInterval.start, requestInterval.end, supplier);
        } else {
            EventQueue.invokeLater(() -> SWEKDownloadManager.workerForcedToStop(this));
        }
    }

    SWEKSupplier getSupplier() {
        return supplier;
    }

    Interval getRequestInterval() {
        return requestInterval;
    }

}
