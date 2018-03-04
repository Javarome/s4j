package org.stellarium.ui.fader;

/**
 * Class which manages a (usually smooth) transition between two states (typically ON/OFF) in function of a counter
 * It used for various purpose like smooth transitions between
 *
 * @author <a href="mailto:javarome@javarome.net">J&eacute;r&ocirc;me Beau</a>
 * @version Java
 */
public abstract class Fader {

    public Fader(boolean someState) {
        this(someState, 0.f, 1.f);
    }

    public Fader(boolean someState, float someMinValue, float someMaxValue) {
        state = someState;
        minValue = someMinValue;
        maxValue = someMaxValue;
    }

    /**
     * Increments the internal counter of delta_time ticks
     */
//    public abstract void update(long deltaTicks);

    /**
     * Gets current switch state
     */
    public abstract float getInterstate();

    public abstract float getInterstatePercentage();

    /**
     * Switchors can be used just as bools
     */
    public abstract Fader set(boolean s);

    public boolean equals(boolean s) {
        return state == s;
    }

    public boolean booleanValue() {
        return state;
    }

    public void setDuration(long someDuration) {
    }

    public void setMinValue(float _min) {
        minValue = _min;
    }

    public void setMaxValue(float _max) {
        maxValue = _max;
    }

    public boolean getState() {
        return state;
    }

    protected boolean state;

    protected float minValue = 0, maxValue = 1;

    public abstract boolean hasInterstate();

    public abstract void update(long deltaTicks);

    public abstract long getDuration();
}
