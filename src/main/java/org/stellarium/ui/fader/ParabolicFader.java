package org.stellarium.ui.fader;

/**
 * Please note that state is updated instantaneously, so if you need to draw something fading in
 * and out, you need to check the interstate value (!=0) to know to draw when on AND during transitions
 */
public class ParabolicFader extends Fader {
    /**
     * Create and initialise to default
     *
     * @param _duration
     * @param _min_value
     * @param _max_value
     * @param _state
     */
    public ParabolicFader(int _duration, float _min_value, float _max_value, boolean _state) {
        super(_state, _min_value, _max_value);
        isTransiting = false;
        duration = _duration;
        interstate = state ? maxValue : minValue;
    }

    public ParabolicFader() {
        this(1000, 0, 1.f, false);
    }

    /**
     * Increments the internal counter of delta_time ticks
     */
    public void update(long delta_ticks) {
        if (!isTransiting) return;// We are not in transition
        counter += delta_ticks;
        if (counter >= duration) {
            // Transition is over
            isTransiting = false;
            interstate = targetValue;
            // state = (target_value==max_value) ? true : false;
        } else {
            interstate = startValue + (targetValue - startValue) * counter / duration;
            interstate *= interstate;
        }

        // printf("Counter %d  interstate %f\n", counter, interstate);
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

        if (isTransiting) {
            // if same end state, no changes
            if (s == state) return this;

            // otherwise need to reverse course
            state = s;
            counter = duration - counter;
            float temp = startValue;
            startValue = targetValue;
            targetValue = temp;

        } else {

            if (state == s) return this;// no change

            // set up and begin transit
            state = s;
            startValue = s ? minValue : maxValue;
            targetValue = s ? maxValue : minValue;
            counter = 0;
            isTransiting = true;
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

    protected long duration;

    protected float startValue, targetValue;

    protected long counter;

    protected float interstate;

    public boolean hasInterstate() {
        return Math.abs(interstate) > 1e-10f;
    }
}
