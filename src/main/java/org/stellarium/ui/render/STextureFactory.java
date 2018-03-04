/*
 * User: freds
 * Date: Nov 9, 2006
 * Time: 12:39:05 AM
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

import org.stellarium.StellariumException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;


public class STextureFactory {
    private static final ConcurrentMap<String, STexture> allTextures = new ConcurrentHashMap<String, STexture>(100, 0.7f, 2);
    private Logger logger;

    public STextureFactory(Logger parentLogger) {
        logger = Logger.getLogger(getClass().getName());
        if (parentLogger != null) {
            logger.setParent(parentLogger);
        }
    }

    // Thinking SMI and Closure, sorry...
    public interface DoCreate {
        public STexture create(Logger parentLogger);
    }

    private STexture getOrCreate(String textureName, DoCreate doCreate) {
        STexture result = allTextures.get(textureName);
        if (result == null) {
            result = doCreate.create(logger);
            allTextures.putIfAbsent(textureName, result);
            // Creating STetxture objects is not heavy but
            // having code using 2 version of same texture is a waste.
            // Actually the C version was doing it, so let's leave it like that
            // result = allTextures.get(textureName);
        }
        return result;
    }

    public STexture createTexture(final String textureName) throws StellariumException {
        return getOrCreate(textureName, new DoCreate() {
            public STexture create(Logger parentLogger) {
                return new STextureJogl(textureName, logger);
            }
        });
    }

    public STexture createTexture(final String textureName,
                                  final int loadType,
                                  final boolean mipmap) throws StellariumException {
        return getOrCreate(textureName, new DoCreate() {
            public STexture create(Logger parentLogger) {
                return new STextureJogl(textureName, loadType, mipmap, logger);
            }
        });
    }

    public STexture createTexture(final String textureName,
                                  final int loadType) throws StellariumException {
        return getOrCreate(textureName, new DoCreate() {
            public STexture create(Logger parentLogger) {
                return new STextureJogl(textureName, loadType, logger);
            }
        });
    }

    public STexture createTexture(final boolean fullpath,
                                  final String textureName,
                                  final int loadType,
                                  final boolean mipmap) throws StellariumException {
        return getOrCreate(textureName, new DoCreate() {
            public STexture create(Logger parentLogger) {
                return new STextureJogl(fullpath, textureName, loadType, mipmap, logger);
            }
        });
    }
}
