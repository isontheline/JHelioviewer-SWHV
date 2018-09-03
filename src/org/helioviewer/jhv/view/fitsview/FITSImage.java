package org.helioviewer.jhv.view.fitsview;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.Cursor;

import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.log.Log;

class FITSImage {

    private static final double GAMMA = 1 / 2.2;
    private static final long BLANK = 0; // marker in case doesn't exist, very unlikely value

    String xml;
    ImageData imageData;

    FITSImage(URI uri) throws Exception {
        try (NetClient nc = NetClient.of(uri); Fits f = new Fits(nc.getStream())) {
            BasicHDU<?>[] hdus = f.read();
            // this is cumbersome
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof CompressedImageHDU) {
                    xml = getHeaderAsXML(hdu.getHeader());
                    readHDU(((CompressedImageHDU) hdu).asImageHDU());
                    return;
                }
            }
            for (BasicHDU<?> hdu : hdus) {
                if (hdu instanceof ImageHDU) {
                    xml = getHeaderAsXML(hdu.getHeader());
                    readHDU(hdu);
                    return;
                }
            }
        }
    }

    private short getValue(short[][] data2D, int j, int i, long blank) {
        short v = data2D[j][i];
        return blank != BLANK && v == blank ? 0 : v;
    }

    private int getValue(int[][] data2D, int j, int i, long blank) {
        int v = data2D[j][i];
        return blank != BLANK && v == blank ? 0 : v;
    }

    private float getValue(float[][] data2D, int j, int i, long blank) {
        float v = data2D[j][i];
        return blank != BLANK && v == blank || Float.isNaN(v) ? 0 : v;
    }

    private float[] sampleImage(int bpp, int width, int height, Object data, long blank) throws Exception {
        int stepW = (width / 1024) * 8;
        int stepH = (height / 1024) * 8;
        float[] sampleData = new float[(width / stepW) * (height / stepH)];

        int k = 0;
        switch (bpp) {
            case BasicHDU.BITPIX_SHORT: {
                short[][] data2D = (short[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW)
                        sampleData[k++] = getValue(data2D, j, i, blank);
                }
                break;
            }
            case BasicHDU.BITPIX_INT: {
                int[][] data2D = (int[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW)
                        sampleData[k++] = getValue(data2D, j, i, blank);
                }
                break;
            }
            case BasicHDU.BITPIX_FLOAT: {
                float[][] data2D = (float[][]) data;
                for (int j = 0; j < height; j += stepH) {
                    for (int i = 0; i < width; i += stepW)
                        sampleData[k++] = getValue(data2D, j, i, blank);
                }
                break;
            }
            default:
                throw new Exception("Bits per pixel not supported: " + bpp);
        }
        return sampleData;
    }

    private void readHDU(BasicHDU<?> hdu) throws Exception {
        int[] axes = hdu.getAxes();
        if (axes.length != 2)
            throw new Exception("Only 2D FITS files supported");
        int height = axes[0];
        int width = axes[1];

        Object pixelData = hdu.getKernel();
        int bpp = hdu.getBitPix();
        if (bpp == BasicHDU.BITPIX_BYTE) {
            byte[][] data2D = (byte[][]) pixelData;
            byte[] byteData = new byte[width * height];
            for (int j = 0; j < height; j++) {
                System.arraycopy(data2D[j], 0, byteData, width * (height - 1 - j), width);
            }
            imageData = new ImageData(width, height, ImageFormat.Gray8, ByteBuffer.wrap(byteData));
        } else {
            long blank = BLANK;
            try {
                blank = hdu.getBlankValue();
            } catch (Exception ignore) {
            }

            float[] sampleData = sampleImage(bpp, width, height, pixelData, blank);
            float[] zLow = {0};
            float[] zHigh = {0};
            float[] zMax = {0};
            ZScale.zscale(sampleData, 0.25f, zLow, zHigh, zMax);

            switch (bpp) {
                case BasicHDU.BITPIX_SHORT: {
                    short[][] data2D = (short[][]) pixelData;
                    int min = (int) zLow[0];
                    int max = (int) zMax[0];

                    PixScale scale = new PowScale(min, max, GAMMA);
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            short v = getValue(data2D, j, i, blank);
                            data[width * (height - 1 - j) + i] = scale.get(v - min);
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    imageData.setGamma(scale.getGamma());
                    break;
                }
                case BasicHDU.BITPIX_INT: {
                    int[][] data2D = (int[][]) pixelData;
                    int min = (int) zLow[0];
                    int max = (int) zMax[0];

                    PixScale scale = new PowScale(min, max, GAMMA);
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            int v = getValue(data2D, j, i, blank);
                            data[width * (height - 1 - j) + i] = scale.get(v - min);
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    imageData.setGamma(scale.getGamma());
                    break;
                }
                case BasicHDU.BITPIX_FLOAT: {
                    float[][] data2D = (float[][]) pixelData;
                    float min = zLow[0];
                    float max = zMax[0];

                    double scale = 65535. / (max == min ? 1 : max - min);
                    short[] data = new short[width * height];
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            float v = getValue(data2D, j, i, blank);
                            data[width * (height - 1 - j) + i] = (short) (scale * Math.pow(v - min, GAMMA));
                        }
                    }
                    imageData = new ImageData(width, height, ImageFormat.Gray16, ShortBuffer.wrap(data));
                    break;
                }
                default:
                    throw new Exception("Bits per pixel not supported: " + bpp);
            }
        }
    }

    private abstract static class PixScale {

        protected static final int MAX_LUT = 1024 * 1024;
        protected short[] lut;

        short get(int v) {
            if (v < 0)
                return lut[0];
            else if (v < lut.length)
                return lut[v];
            else
                return lut[lut.length - 1];
        }

        abstract double getGamma();

    }

    private static class LinScale extends PixScale {

        LinScale(int min, int max) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / diff;

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * i + .5);
        }

        @Override
        double getGamma() {
            return GAMMA;
        }

    }


    private static class LogScale extends PixScale {

        LogScale(int min, int max) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / Math.log1p(diff);

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.log1p(i) + .5);
        }

        @Override
        double getGamma() {
            return 1;
        }

    }

    private static class PowScale extends PixScale {

        PowScale(int min, int max, double p) {
            int diff = max > min ? max - min : 1;
            if (diff > MAX_LUT) {
                Log.debug("Pixel scaling LUT too big: " + diff);
                diff = MAX_LUT;
            }
            double scale = 65535. / Math.pow(diff, p);

            lut = new short[diff + 1];
            for (int i = 0; i < lut.length; i++)
                lut[i] = (short) (scale * Math.pow(i, p) + .5);
        }

        @Override
        double getGamma() {
            return 1;
        }

    }

    private static String getHeaderAsXML(Header header) {
        String nl = System.getProperty("line.separator");
        StringBuilder builder = new StringBuilder("<meta>").append(nl).append("<fits>").append(nl);

        for (Cursor<String, HeaderCard> iter = header.iterator(); iter.hasNext(); ) {
            HeaderCard headerCard = iter.next();
            if (headerCard.getValue() != null) {
                builder.append('<').append(headerCard.getKey()).append('>').append(headerCard.getValue()).append("</").append(headerCard.getKey()).append('>').append(nl);
            }
        }
        builder.append("</fits>").append(nl).append("</meta>");
        return builder.toString().replace("&", "&amp;");
    }

}
