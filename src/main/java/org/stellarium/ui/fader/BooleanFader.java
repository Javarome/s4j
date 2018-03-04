package org.stellarium.ui.fader;

/*
* Stellarium
* Copyright (C) 2005 Fabien Ch�reau
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

class BooleanFader extends Fader {

    public BooleanFader(boolean _state) {
        super(_state);
    }

    // Create and initialise
    public BooleanFader(boolean _state, float _min_value, float _max_value) {
        super(_state, _min_value, _max_value);
    }

    // Increments the internal counter of delta_time ticks
    public void update(long delta_ticks) {
    }

    // Gets current switch state
    public float getInterstate() {
        return state ? maxValue : minValue;
    }

    public float getInterstatePercentage() {
        return state ? 100.f : 0.f;
    }

    // Switchors can be used just as bools
    public Fader set(boolean s) {
        state = s;
        return this;
    }

    public boolean hasInterstate() {
        return true;
    }

    public long getDuration() {
        return 0;
    }
}

/* better idea but not working...
class parabolic_fader : public linear_fader
{
public:
    parabolic_fader(int _duration=1000, float _min_value=0.f, float _max_value=1.f, bool _state=false)
        : linear_fader(_duration, _min_value, _max_value, _state)
        {
        }

    // Increments the internal counter of delta_time ticks
    void update(int delta_ticks)
    {

        printf("Counter %d  interstate %f\n", counter, interstate);
        if (!is_transiting) return; // We are not in transition
        counter+=delta_ticks;
        if (counter>=duration)
        {
            // Transition is over
            is_transiting = false;
            interstate = target_value;
        }
        else
        {
            interstate = start_value + (target_value - start_value) * counter/duration;
            interstate *= interstate;
        }

        printf("Counter %d  interstate %f\n", counter, interstate);
    }
};
*/
