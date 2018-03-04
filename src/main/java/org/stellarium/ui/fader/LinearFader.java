/*
* Stellarium
* Copyright (C) 2002 Fabien Chereau
*
* Stellarium for Java
* Copyright (C) 2008 Jerome Beau, Frederic Simon
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
package org.stellarium.ui.fader;

/**
 * A fader that changes its state in a linear way.
 * <p/>
 * Please note that state is updated instantaneously, so if you need to draw something fading in
 * and out, you need to check the interstate value (!=0) to know to draw when on AND during transitions
 *
 * @author Fabien Chereau for the original C++ version
 * @author <a href="mailto:rr0@rr0.org">J&eacute;r&ocirc;me Beau</a>
 */
public class LinearFader extends Fader {
    private long waitTime;

    /**
     * Create and initialise to default
     */
    public LinearFader() {
        this(1000, 0, 1, false, 100);
    }

    /**
     * Create and initialise to default
     *
     * @param someDuration The fading duration, in milliseconds.
     * @param someMinValue The value to start from.
     * @param someMaxValue The value to reach at the end.
     * @param someState    The state.
     * @param waitTime     The time to wait between each interstate, in milliseconds
     */
    public LinearFader(long someDuration, float someMinValue, float someMaxValue, boolean someState, long waitTime) {
        super(someState, someMinValue, someMaxValue);
        this.waitTime = waitTime;
        interstate = state ? maxValue : minValue;
        setDuration(someDuration);
    }

    /**
     * Increments the internal counter of delta_time ticks
     *
     * @param deltaTicks
     */
    public void update(long deltaTicks) {
        if (!isTransiting) {
            return;// We are not in transition
        }
        counter += deltaTicks;
        if (counter >= duration) {
            // Transition is over
            isTransiting = false;
            interstate = targetValue;
            // state = (target_value==max_value) ? true : false;
        } else {
            interstate = startValue + (targetValue - startValue) * counter / duration;
        }
//        System.out.println("counter=" + counter + ", interstate=" + interstate);
    }

    /**
     * Get current switch state
     */
    public float getInterstate() {
        return interstate;
    }

    public float getInterstatePercentage() {
        return 100.f * (interstate - minValue) / (maxValue - minValue);
    }

    /**
     * Faders can be used just as bools
     */
    public Fader set(boolean s) {
        if (s == state) {
            return this;
        }
        if (isTransiting) {
            // if same end state, no changes

            // otherwise need to reverse course
            state = s;
            counter = duration - counter;
            float temp = startValue;
            startValue = targetValue;
            targetValue = temp;

        } else {
            // set up and begin transit
            state = s;
            startValue = s ? minValue : maxValue;
            targetValue = s ? maxValue : minValue;
            counter = 0;
            isTransiting = true;
            update(0);
            new Thread(new Runnable() {
                public synchronized void run() {
                    while (counter < duration) {
                        try {
                            wait(waitTime);
                            update(waitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        return this;
    }

    public void setDuration(long someDuration) {
        duration = someDuration;
    }

    public long getDuration() {
        return duration;
    }

    public void setMinValue(float _min) {
        minValue = _min;
    }

    public void setMaxValue(float _max) {
        maxValue = _max;
    }

    protected boolean isTransiting;

    protected long duration = 1000;

    protected float startValue, targetValue;

    protected long counter;

    protected float interstate;

    public boolean hasInterstate() {
        return Math.abs(interstate) > 1e-10f;
    }
}
