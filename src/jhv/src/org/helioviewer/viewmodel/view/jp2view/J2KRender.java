package org.helioviewer.viewmodel.view.jp2view;

import java.awt.EventQueue;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduConstants;
import org.helioviewer.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * The J2KRender class handles all of the decompression, buffering, and
 * filtering of the image data. It essentially just waits for the shared object
 * in the JP2ImageView to signal it.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Desmond Amadigwe
 * @author Markus Langenberg
 */
class J2KRender implements Runnable {

    /** A reference to the JP2Image this object is owned by. */
    private final JP2Image parentImageRef;

    /** A reference to the JP2ImageView this object is owned by. */
    private final JHVJP2View parentViewRef;

    /** A reference to the compositor used by this JP2Image. */
    private final Kdu_region_compositor compositorRef;

    /** An integer buffer used in the run method. */
    static private int[] localIntBuffer = new int[0];
    private int[] intBuffer;
    private int currentIntBuffer = 0;

    /** A byte buffer used in the run method. */
    private byte[] byteBuffer;
    private int currentByteBuffer = 0;

    /** Maximum of samples to process per rendering iteration */
    private final int MAX_RENDER_SAMPLES = 50000;

    private final JP2ImageParameter currParams;

    J2KRender(JHVJP2View _parentViewRef, JP2Image _parentImageRef, JP2ImageParameter _currParams) {
        currParams = _currParams;

        parentViewRef = _parentViewRef;
        parentImageRef = _parentImageRef;
        compositorRef = parentImageRef.getCompositorRef();
    }

    private void renderLayer() {

        try {
            int numLayer = currParams.compositionLayer;

            // see TODO below
            // compositorRef.Refresh();
            // compositorRef.Remove_compositing_layer(-1, true);

            // not needed: the raw component is extracted from codestream
            // parentImageRef.deactivateColorLookupTable(numLayer);

            // TODO: figure out for getNumComponents() > 2
            // Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
            // compositorRef.Add_compositing_layer(numLayer, dimsRef1,
            // dimsRef2);

            parentImageRef.updateResolutionSet(numLayer);

            compositorRef.Set_surface_initialization_mode(false);
            compositorRef.Set_scale(false, false, false, currParams.resolution.getZoomPercent());
            if (parentImageRef.getNumComponents() <= 2) {
                compositorRef.Set_single_component(numLayer, 0, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS);
            }

            SubImage roi = currParams.subImage;
            Kdu_dims requestedBufferedRegion = KakaduUtils.roiToKdu_dims(roi);
            compositorRef.Set_buffer_surface(requestedBufferedRegion, 0);

            Kdu_dims actualBufferedRegion = new Kdu_dims();
            Kdu_compositor_buf compositorBuf = compositorRef.Get_composition_buffer(actualBufferedRegion);

            Kdu_coords actualOffset = new Kdu_coords();
            actualOffset.Assign(actualBufferedRegion.Access_pos());

            Kdu_dims newRegion = new Kdu_dims();

            if (parentImageRef.getNumComponents() < 2) {
                currentByteBuffer = (currentByteBuffer + 1);
                byteBuffer = new byte[roi.getNumPixels()];
            } else {
                currentIntBuffer = (currentIntBuffer + 1);
                intBuffer = new int[roi.getNumPixels()];
            }

            while (!compositorRef.Is_processing_complete()) {
                compositorRef.Process(MAX_RENDER_SAMPLES, newRegion);
                Kdu_coords newOffset = newRegion.Access_pos();
                Kdu_coords newSize = newRegion.Access_size();

                newOffset.Subtract(actualOffset);

                int newWidth = newSize.Get_x();
                int newHeight = newSize.Get_y();
                int newPixels = newWidth * newHeight;

                if (newPixels == 0) {
                    continue;
                }

                localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1] : localIntBuffer;
                compositorBuf.Get_region(newRegion, localIntBuffer);

                int srcIdx = 0;
                int destIdx = newOffset.Get_x() + newOffset.Get_y() * roi.width;

                if (parentImageRef.getNumComponents() < 2) {
                    for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                        for (int col = 0; col < newWidth; ++col) {
                            byteBuffer[destIdx + col] = (byte) (localIntBuffer[srcIdx + col] & 0xFF);
                        }
                    }
                } else {
                    for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                        System.arraycopy(localIntBuffer, srcIdx, intBuffer[currentIntBuffer], destIdx, newWidth);
                    }
                }
            }

            if (parentImageRef.getNumComponents() == 2) {
                // extract alpha component
                compositorRef.Set_single_component(numLayer, 1, KakaduConstants.KDU_WANT_CODESTREAM_COMPONENTS);
                while (!compositorRef.Is_processing_complete()) {
                    compositorRef.Process(MAX_RENDER_SAMPLES, newRegion);
                    Kdu_coords newOffset = newRegion.Access_pos();
                    Kdu_coords newSize = newRegion.Access_size();

                    newOffset.Subtract(actualOffset);

                    int newWidth = newSize.Get_x();
                    int newHeight = newSize.Get_y();
                    int newPixels = newWidth * newHeight;

                    if (newPixels == 0) {
                        continue;
                    }

                    localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1] : localIntBuffer;
                    compositorBuf.Get_region(newRegion, localIntBuffer);

                    int srcIdx = 0;
                    int destIdx = newOffset.Get_x() + newOffset.Get_y() * roi.width;

                    for (int row = 0; row < newHeight; row++, destIdx += roi.width, srcIdx += newWidth) {
                        for (int col = 0; col < newWidth; ++col) {
                            intBuffer[destIdx + col] = (intBuffer[destIdx + col] & 0x00FFFFFF) | ((localIntBuffer[srcIdx + col] & 0x00FF0000) << 8);
                        }
                    }
                }
            }

            if (compositorBuf != null) {
                compositorBuf.Native_destroy();
            }

        } catch (KduException e) {
            e.printStackTrace();
        }

    }

    private static final Object renderLock = new Object();

    @Override
    public void run() {
        synchronized (renderLock) {
            renderLayer();
        }
        SubImage roi = currParams.subImage;
        int width = roi.width;
        int height = roi.height;
        ImageData imdata = null;

        if (parentImageRef.getNumComponents() < 2) {
            imdata = new SingleChannelByte8ImageData(width, height, byteBuffer);
        } else {
            boolean singleChannel = false;
            if (parentImageRef.getNumComponents() == 2) {
                singleChannel = true;
            }
            imdata = new ARGBInt32ImageData(singleChannel, width, height, intBuffer);
        }
        setImageData(imdata, currParams);

    }

    private void setImageData(ImageData newImdata, JP2ImageParameter newParams) {
        EventQueue.invokeLater(new Runnable() {
            private ImageData theImdata;
            private JP2ImageParameter theParams;

            @Override
            public void run() {
                parentViewRef.setSubimageData(theImdata, theParams);
            }

            public Runnable init(ImageData theImdata, JP2ImageParameter theParams) {
                this.theImdata = theImdata;
                this.theParams = theParams;
                return this;
            }
        }.init(newImdata, newParams));
    }

}
