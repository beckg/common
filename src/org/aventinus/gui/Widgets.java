//-----------------------------------------------------------------------------------
// Copyright (c) 2009-2013, Gordon Beck (gordon.beck@aventinus.org). All rights reserved.
//
//    This file is part of a suite of tools. 
//
//    The tools are free software: you can redistribute it and/or modify 
//    it under the terms of the GNU General Public License as published by 
//    the Free Software Foundation, either version 3 of the License, or 
//    (at your option) any later version. 
// 
//    The tools are distributed in the hope that they will be useful, 
//    but WITHOUT ANY WARRANTY; without even the implied warranty of 
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
//    GNU General Public License for more details. 
// 
//    You should have received a copy of the GNU General Public License 
//    along with these tools.  If not, see <http://www.gnu.org/licenses/>.
//-----------------------------------------------------------------------------------
package org.aventinus.gui;

import java.util.*;
import java.util.List;
import java.io.*;
import java.text.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.Graphics2D.*;
import javax.swing.*;
import javax.swing.border.*;

//------------------------------------------------------------------
//
//------------------------------------------------------------------
public class Widgets
{
    //-------------------------------------------------------------------------------
    // These widgets size properly to the preferred size in nested BoxLayout arrangements
    //-------------------------------------------------------------------------------
    public static class SizedLabel extends JLabel
    {
        static final long serialVersionUID = 1;

        public SizedLabel(String label)
        {
            super(label);
   
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            return getPreferredSize();
        }
    }

    public static class SizedCheckBox extends JCheckBox
    {
        static final long serialVersionUID = 1;

        public SizedCheckBox(String label)
        {
            super(label);
   
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            return getPreferredSize();
        }
    }

    public static class SizedTextField extends JTextField
    {
        static final long serialVersionUID = 1;

        private boolean setmax = false;

        public SizedTextField(int characters)
        {
            this(characters, true);
        }

        public SizedTextField(int characters, boolean setmax)
        {
            super(characters);

            this.setmax = setmax;
   
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            if (setmax)
            {
                return getPreferredSize();
            }
            else
            {
                return super.getMaximumSize();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public static class SizedComboBox extends JComboBox
    {
        static final long serialVersionUID = 1;

        private boolean setmax = false;

        public SizedComboBox(Object[] values)
        {
            this(values, true);
        }

        @SuppressWarnings("unchecked")
        public SizedComboBox(Object[] values, boolean setmax)
        {
            for (Object value : values)
               addItem(value);

            this.setmax = setmax;

//            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            if (setmax)
            {
                return getPreferredSize();
            }
            else
            {
                return super.getMaximumSize();
            }
        }
    }

    public static class SizedTextArea extends JTextArea
    {
        static final long serialVersionUID = 1;

        private boolean setmax = false;

        public SizedTextArea()
        {
            this(true);
        }

        public SizedTextArea(boolean setmax)
        {
            super();

            this.setmax = setmax;
   
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            if (setmax)
            {
                return getPreferredSize();
            }
            else
            {
                return super.getMaximumSize();
            }
        }
    }

    public static class SizedScrollPane extends JScrollPane
    {
        static final long serialVersionUID = 1;

        private boolean setmaxV = false;
        private boolean setmaxH = false;

        public SizedScrollPane(JComponent view)
        {
            this(view, true, true);
        }

        public SizedScrollPane(JComponent view, boolean setmaxH, boolean setmaxV)
        {
            super(view);

            this.setmaxV = setmaxV;
            this.setmaxH = setmaxH;
   
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public Dimension getMaximumSize()
        {
            int width = super.getMaximumSize().width;
            int height = super.getMaximumSize().height;
            if (setmaxH)
                 width = getPreferredSize().width;
            if (setmaxV)
                 height = getPreferredSize().height;

            return new Dimension(width, height);
        }
    }

    public static class LabeledField extends JPanel
    {
        static final long serialVersionUID = 1;

        private JTextField field;

        public LabeledField(String text, int characters)
        {
            this(text, characters, true);
        }

        public LabeledField(String text, int characters, boolean setmax)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JLabel label = new SizedLabel(text);
            add(label);
            field = new SizedTextField(characters, setmax);
            add(field);
        }

        public JTextField getTextField()
        {
            return field;
        }
    }

    public static class LabeledComboBox extends JPanel
    {
        static final long serialVersionUID = 1;

        @SuppressWarnings("rawtypes")
        private JComboBox comboBox;

        public LabeledComboBox(String text, Object[] values)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JLabel label = new SizedLabel(text);
            add(label);
            @SuppressWarnings("rawtypes")
            JComboBox comboBox = new SizedComboBox(values);
            this.comboBox = comboBox;
            add(comboBox);
        }

        @SuppressWarnings("rawtypes")
        public JComboBox getComboBox()
        {
            return comboBox;
        }
    }

    public static class LabeledScrollableArea extends JPanel
    {
        static final long serialVersionUID = 1;

        private JTextArea area;

        public LabeledScrollableArea(String text, int width, int height)
        {
            this(text, width, height, true, true);
        }

        public LabeledScrollableArea(String text, int width, int height, boolean setmaxH, boolean setmaxV)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JLabel label = new SizedLabel(text);
            add(label);

            area = new JTextArea();
            JScrollPane pane = new SizedScrollPane(area, setmaxH, setmaxV);
            pane.setPreferredSize(new Dimension(width, height));
            add(pane);
        }

        public JTextArea getTextArea()
        {
            return area;
        }
    }

    //-------------------------------------------------------------------------------
    //
    //-------------------------------------------------------------------------------
    public static class DummyGraphics extends Graphics2D
    {
        public void addRenderingHints(Map<?,?> hints) {}
        public void clip(Shape s) {}
        public void draw(Shape s) {}
        public void drawGlyphVector(GlyphVector g, float x, float y) {}
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {}
        public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {return false;}
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {}
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {}
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {}
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {}
        public void drawString(String str, float x, float y) {}
        public void drawString(String str, int x, int y) {}
        public void fill(Shape s) {}
        public Color getBackground(){return null;} 
        public Composite getComposite() {return null;}
        public GraphicsConfiguration getDeviceConfiguration() {return null;}
        public FontRenderContext getFontRenderContext() {return null;}
        public Paint getPaint() {return null;}
        public Object getRenderingHint(RenderingHints.Key hintKey) {return null;}
        public RenderingHints getRenderingHints() {return null;}
        public Stroke getStroke() {return null;}
        public AffineTransform getTransform() {return new AffineTransform();}
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {return false;}
        public void rotate(double theta) {}
        public void rotate(double theta, double x, double y) {}
        public void scale(double sx, double sy) {}
        public void setBackground(Color color) {}
        public void setComposite(Composite comp) {}
        public void setPaint(Paint paint) {}
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {}
        public void setRenderingHints(Map<?,?> hints) {}
        public void setStroke(Stroke s) {}
        public void setTransform(AffineTransform Tx) {}
        public void shear(double shx, double shy) {}
        public void transform(AffineTransform Tx) {}
        public void translate(double tx, double ty) {}
        public void translate(int x, int y) {}

        public void clearRect(int x, int y, int width, int height) {}
        public void clipRect(int x, int y, int width, int height) {}
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {}
        public Graphics create() {return null;}
        public void dispose() {}
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {return false;}
        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {return false;}
        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {return false;}
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {return false;}
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {return false;}
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {return false;}
        public void drawLine(int x1, int y1, int x2, int y2) {}
        public void drawOval(int x, int y, int width, int height) {}
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {}
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {}
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
        public void fillOval(int x, int y, int width, int height) {}
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {}
        public void fillRect(int x, int y, int width, int height) {}
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
        public Shape getClip() {return null;}
        public Rectangle getClipBounds() {return null;}
        public Color getColor() {return null;}
        public Font getFont() {return null;}
        public FontMetrics getFontMetrics(Font f) {return null;}
        public void setClip(int x, int y, int width, int height) {}
        public void setClip(Shape clip) {}
        public void setColor(Color c) {}
        public void setFont(Font font) {}
        public void setPaintMode() {}
        public void setXORMode(Color c1) {}
    }
}
