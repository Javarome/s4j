/*
 * Stellarium
 * This file Copyright (C) 2004 Robert Spearman
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
package org.stellarium.astro;

import org.stellarium.Navigator;
import org.stellarium.ToneReproductor;
import org.stellarium.projector.DefaultProjector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MeteorMgr {

    public MeteorMgr(int zhr, int maxv) {
        ZHR = zhr;
        maxVelocity = maxv;

        // calculate factor for meteor creation rate per second since visible area ZHR is for
        // estimated visible radius of 458km
        // (calculated for average meteor magnitude of +2.5 and limiting magnitude of 5)

        //  zhr_to_wsr = 1.0f/3600.f;
        zhrToWsr = 1.6667f / 3600.f;
        // this is a correction factor to adjust for the model as programmed to match observed rates
    }

    public void setZHR(int zhr) {
        ZHR = zhr;
    }

    public int getZHR() {
        return ZHR;
    }

    void setMaxVelocity(int maxv) {
        maxVelocity = maxv;
    }

    public void update(DefaultProjector proj, Navigator nav, ToneReproductor eye, int deltaTime) {
        // step through and update all active meteors
        //        int n = 0;
        Iterator<Meteor> activeIterator = active.iterator();
        while (activeIterator.hasNext()) {
            Meteor iter = activeIterator.next();
            //            n++;
            //printf("Meteor %d update\n", ++n);
            if (!iter.update(deltaTime)) {
                // remove dead meteor
                //      printf("Meteor \tdied\n");
                activeIterator.remove();
            }
        }

        // only makes sense given lifetimes of meteors to draw when time_speed is realtime
        // otherwise high overhead of large numbers of meteors
        double tspeed = nav.getTimeSpeed() * 86400;// sky seconds per actual second
        if (tspeed <= 0 || Math.abs(tspeed) > 1) {
            // don't start any more meteors
            return;
        }

        /*
        // debug - one at a time
        if(active.begin() == active.end() ) {
          Meteor *m = new Meteor(projection, navigation, max_velocity);
          active.add(m);
        }
        */

        // if stellarium has been suspended, don't create huge number of meteors to
        // make up for lost time!
        if (deltaTime > 500) {
            deltaTime = 500;
        }

        // determine average meteors per frame needing to be created
        int mpf = (int) ((double) ZHR * zhrToWsr * (double) deltaTime / 1000.0f + 0.5);
        if (mpf < 1) mpf = 1;

        int mlaunch = 0;
        for (int i = 0; i < mpf; i++) {

            // start new meteor based on ZHR time probability
            double prob = Math.random();
            if (ZHR > 0 && prob < ((double) ZHR * zhrToWsr * (double) deltaTime * (double) mpf / 1000.0f)) {
                Meteor m = new Meteor(proj, nav, eye, maxVelocity);
                active.add(m);
                mlaunch++;
            }
        }

        //  printf("mpf: %d\tm launched: %d\t(mps: %f)\t%d\n", mpf, mlaunch, ZHR*zhr_to_wsr, deltaTime);
    }

    public void draw(DefaultProjector proj, Navigator nav) {
        if (!active.isEmpty()) {
            proj.setOrthographicProjection();
            try {
                // step through and draw all active meteors
                for (Meteor iter : active) {
                    iter.draw(proj, nav);
                }
            } finally {
                proj.resetPerspectiveProjection();
            }
        }
    }

    /**
     * All active meteors
     */
    private List<Meteor> active = new ArrayList<Meteor>();

    int ZHR;

    int maxVelocity;

    /**
     * factor to convert from zhr to whole earth per second rate
     */
    double zhrToWsr;
}
