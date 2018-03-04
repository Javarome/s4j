/*
 * Stellarium
 * This file Copyright (C) 2005 Robert Spearman
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
package org.stellarium.ui.render;

import org.stellarium.NavigatorIfc;
import org.stellarium.StellariumException;
import org.stellarium.projector.Projector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * manage an assortment of script loaded images
 */
public class ImageMgr {

    protected Logger logger;
    private List<Image> activeImages = new ArrayList<Image>();

    public ImageMgr(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        dropAllImages();
    }

    public boolean loadImage(String filename, String name, Image.IMAGE_POSITIONING position_type) throws StellariumException {
        // if name already exists, replace with new image (hash would have been easier...)
        for (Image iter : activeImages) {
            if (iter.getName().equals(name)) {
                activeImages.remove(iter);
            }
        }

        Image img = new Image(filename, name, position_type, logger);
        if (img.imageLoaded())
            activeImages.add(img);
        return true;
    }

    public boolean dropImage(String name) {
        for (Image iter : activeImages) {
            if (iter.getName().equals(name)) {
                activeImages.remove(iter);
                return true;
            }
        }
        return false;// not found
    }

    public boolean dropAllImages() {
        for (Image iter : activeImages) {
            //
        }
        activeImages.clear();
        return true;
    }

    public Image getImage(String name) {
        for (Image iter : activeImages) {
            if (iter.getName().equals(name)) return iter;
        }
        return null;
    }

    public void update(long deltaTime) {
        for (Image iter : activeImages) {
            iter.update(deltaTime);
        }
    }

    public void draw(NavigatorIfc nav, Projector prj) {
        if (!activeImages.isEmpty()) {
            prj.setOrthographicProjection();
            try {
                for (Image iter : activeImages) {
                    iter.draw(nav, prj);
                }
            } finally {
                prj.resetPerspectiveProjection();
            }
        }
    }
}