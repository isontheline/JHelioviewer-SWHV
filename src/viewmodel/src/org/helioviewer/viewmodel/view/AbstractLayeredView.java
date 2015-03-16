package org.helioviewer.viewmodel.view;

import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Abstract base class implementing LayeredView, providing some common
 * functions.
 *
 * <p>
 * This class provides most of the functionality of a LayeredView, since most of
 * its behavior is independent from the render mode. Because of that, the whole
 * management of the stack of layers is centralized in this abstract class.
 * <p>
 * To improve performance, many intermediate results are cached.
 * <p>
 * For further informations about how to use layers, see {@link LayeredView}.
 *
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 *
 */
public abstract class AbstractLayeredView extends AbstractView implements LayeredView, RegionView, ViewportView, MetaDataView, ViewListener {

    protected ArrayList<View> layers = new ArrayList<View>();
    protected HashMap<View, Layer> viewLookup = new HashMap<View, Layer>();

    protected ViewportImageSize viewportImageSize;

    protected Viewport viewport;
    protected Region region;
    protected MetaData metaData;

    private double minimalRegionSize;

    /**
     * Buffer for precomputed values for each layer.
     *
     * <p>
     * This container saves some values per layer, such as its visibility and
     * precomputed view adapters.
     *
     */
    protected class Layer {
        public final View view;

        public RegionView regionView;
        public ViewportView viewportView;
        public MetaDataView metaDataView;
        public SubimageDataView subimageDataView;

        public boolean visibility = true;

        /**
         * Default constructor.
         *
         * Computes view adapters for this layer.
         *
         * @param base
         *            layer to save
         */
        public Layer(View base) {
            view = base;
            update();
        }

        /**
         * Recalculates the view adapters, in case the view chain has changed.
         */
        public void update() {
            if (view != null) {
                regionView = view.getAdapter(RegionView.class);
                viewportView = view.getAdapter(ViewportView.class);
                metaDataView = view.getAdapter(MetaDataView.class);
                subimageDataView = view.getAdapter(SubimageDataView.class);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(View view) {
        Layer layer = viewLookup.get(view);

        if (layer != null)
            return layer.visibility;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfVisibleLayer() {
        int result = 0;
        for (Layer layer : viewLookup.values()) {
            if (layer.visibility) {
                result++;
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleVisibility(View view) {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        Layer layer = viewLookup.get(view);
        if (layer != null) {
            layer.visibility = !layer.visibility;
            redrawBuffer(new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_VISIBILITY, view)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(View newLayer) {
        addLayer(newLayer, layers.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null) {
            return;
        }
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_ADDED, newLayer));

        layers.add(newIndex, newLayer);
        newLayer.addViewListener(this);

        viewLookup.put(newLayer, new Layer(newLayer));

        redrawBuffer(changeEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            return layers.get(index);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLayerLevel(View view) {
        return layers.indexOf(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(View view) {
        removeLayer(view, true);
    }

    @Override
    public void removeLayer(View view, boolean needAbolish) {
        if (view == null) {
            return;
        }

        int index = layers.indexOf(view);
        if (index == -1) {
            return;
        }
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        layers.remove(view);
        viewLookup.remove(view);
        view.removeViewListener(this);

        JHVJP2View jhvjp2 = view.getAdapter(JHVJP2View.class);
        if (jhvjp2 != null) {
            if (needAbolish) {
                jhvjp2.abolish();
            }
            jhvjp2.removeRenderListener();
        }

        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));

        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            removeLayer(layers.get(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllLayers() {
        ChangeEvent event = new ChangeEvent();

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
        for (View view : layers) {
            int index = layers.indexOf(view);
            view.removeViewListener(this);
            if (view.getAdapter(JHVJP2View.class) != null) {
                view.getAdapter(JHVJP2View.class).abolish();
            }
            event.addReason(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));
        }

        layers.clear();
        viewLookup.clear();

        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings(value = { "unchecked" })
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this)) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRegion(Region r, ChangeEvent event) {

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            for (Layer layer : viewLookup.values()) {
                layer.update();
            }
        }

        if ((aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class) || aEvent.reasonOccurred(ViewChainChangedReason.class)) && sender != null) {
            redrawBuffer(aEvent);
        } else {
            notifyViewListeners(aEvent);
        }
    }

    /**
     * Redraws the scene.
     *
     * Calls the implementation specific function redrawBufferImpl(). A
     * SubImageDataChangedReason will be appended to the given ChangeEvent.
     *
     * @param aEvent
     *            ChangeEvent to collect history
     */
    protected void redrawBuffer(ChangeEvent aEvent) {
        notifyViewListeners(aEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveView(View view, int newLevel) {
        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_MOVED, view, newLevel));

        if (layers.contains(view)) {
            layers.remove(view);
            layers.add(newLevel, view);
            redrawBuffer(changeEvent);
        }
    }

    @Override
    public boolean setViewport(Viewport v, ChangeEvent event) {
        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;

        if (!setRegion(region, event)) {
            redrawBuffer(event);
        }
        return true;
    }
}
