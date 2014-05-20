package org.helioviewer.gl3d.scenegraph.visuals;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.media.opengl.GL;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;

public class GL3DFont {

    static GL3DFont instance = new GL3DFont();
    HashMap<String, Integer> loadedFontsTextureId = new HashMap<String, Integer>();
    HashMap<String, RectangleDouble[]> loadedFontsCharacterPosition = new HashMap<String, RectangleDouble[]>();
    int fontSize = 64;
    public static GL3DFont getSingletonInstance() {
        if (instance == null) {
            instance = new GL3DFont();
        }
        return instance;
    }

    public void loadFont(String font, GL gl) {
        int texture_id;
        if (!loadedFontsTextureId.containsKey(font)) {
            BufferedImage img = getFontBufferedImage(font);
            GLTextureHelper th = new GLTextureHelper();
            texture_id = th.genTextureID(gl);
            th.moveBufferedImageToGLTexture(gl, img, texture_id);
            loadedFontsTextureId.put(font, texture_id);
        }
        texture_id = loadedFontsTextureId.get(font);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture_id);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
    }

    public RectangleDouble[] getCharacters(String fontName){
        if (!this.loadedFontsCharacterPosition.containsKey(fontName)) {
            this.getFontBufferedImage(fontName);
        }
        return this.loadedFontsCharacterPosition.get(fontName);

    }

    private BufferedImage getFontBufferedImage(String font) {
        BufferedImage img = new BufferedImage(256, 512, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = img.createGraphics();
        g2d.setPaint(Color.blue);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(new Font(font, Font.PLAIN, fontSize));
        RectangleDouble[] rects= new RectangleDouble[256];
        FontMetrics fm = g2d.getFontMetrics();

        int maxWidth = 0;
        int maxHeight = 0;
        int width;
        int height;
        for(int i=0; i<256; i++){
            char c = ((char)i);
            String s = Character.toString(c);
            Rectangle2D sb = fm.getStringBounds(s, g2d);
            width =(int)sb.getWidth();
            if(width>maxWidth){
                maxWidth = width;
            }
            height = (int)sb.getHeight();
            if(height>maxHeight){
                maxHeight = height;
            }
        }
        int currentX = 0;
        int currentY = maxHeight;

        char c;
        String s ;
        for(int i=0; i<256; i++){
            c = ((char)i);
            s = Character.toString(c);
            Rectangle2D sb = fm.getStringBounds(s, g2d);
            width =(int)sb.getWidth();
            height = (int)sb.getHeight();

            if(currentX + width>=256){
                currentX = 0;
                currentY +=maxHeight;
            }
            RectangleDouble rect = new org.helioviewer.base.math.RectangleDouble((currentX)/256., (currentY + fontSize/5.)/512., width/256., height/512.);
            rects[i] = rect;
            g2d.drawString(s, currentX, currentY);


                currentX += width;
        }
        loadedFontsCharacterPosition.put(font, rects);
        for(int i=0; i<256; i++){
            //System.out.println(rects[i]);
        }
        return img;
    }


      public static void main(String[] args)
      {
        String fonts[] =
          GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        for ( int i = 0; i < fonts.length; i++ )
        {
          System.out.println(fonts[i]);
        }
      }


}
