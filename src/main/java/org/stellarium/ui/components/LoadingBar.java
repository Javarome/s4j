/*
 * Stellarium
 * Copyright (C) 2005 Fabien Chereau
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.stellarium.ui.components;

import org.stellarium.StellariumException;
import org.stellarium.data.ResourceLocatorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Display loading bar
 */
public class LoadingBar implements PropertyChangeListener {

    private JProgressBar progressBar;
    private JWindow window;

    public LoadingBar(String splashTexture, int screenw, int screenh, final String extraTextString, int extraTextSize,
                      final int extraTextPosx, final int extraTextPosy) throws StellariumException {
        window = new JWindow();
        window.setLayout(new BorderLayout());

        final ImageObserver imageObserver = null;
        final ImageIcon logo = new ImageIcon(ResourceLocatorUtil.getInstance().getTextureURL(splashTexture));
        final Image image = logo.getImage();
        final int imageWidth = image.getWidth(imageObserver);
        final int imageHeight = image.getHeight(imageObserver);
        final Font extraTextFont = new Font("Sans", Font.PLAIN, extraTextSize);
        Canvas content = new Canvas() {
            public void paint(Graphics g) {
                g.drawImage(image, 0, 0, imageObserver);
                g.setColor(Color.WHITE);
                g.setFont(extraTextFont);
                g.drawString(extraTextString, extraTextPosx, extraTextPosy);
            }

            public Dimension getPreferredSize() {
                return new Dimension(imageWidth, imageHeight);
            }
        };
        window.add(content, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        window.add(progressBar, BorderLayout.SOUTH);

        window.pack();
        window.setLocation((screenw - window.getWidth()) / 2, (screenh - window.getHeight()) / 2);
        window.setAlwaysOnTop(true);
        window.setVisible(true);
    }

    public void close() {
        window.dispose();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Float val = (Float) evt.getNewValue();
        progressBar.setString(evt.getPropertyName());
        final int i = (int) (val * 100f);
        progressBar.setValue(i);
    }
}