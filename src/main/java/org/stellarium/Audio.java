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
package org.stellarium;

import javax.sound.sampled.*;
import java.io.File;

/**
 * manage an audio track (SDL mixer music track)
 */
public class Audio {
    public Audio(String filename, String name) throws StellariumException {
        trackName = name;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filename));

            // At present, ALAW and ULAW encodings must be converted
            // to PCM_SIGNED before it can be played
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        format.getSampleSizeInBits() * 2,
                        format.getChannels(),
                        format.getFrameSize() * 2,
                        format.getFrameRate(),
                        true);// big endian
                stream = AudioSystem.getAudioInputStream(format, stream);
            }

            // Create the clip
            DataLine.Info info = new DataLine.Info(
                    Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            track = (Clip) AudioSystem.getLine(info);
            // This method does not return until the audio file is completely loaded
            track.open(stream);
        } catch (Exception e) {
            throw new StellariumException("Could not read audio clip", e);
        }
    }

    public Audio(String s, String s1, long l) {
        // TODO
    }

    protected void finalize() {
        stop();// stop playing
        track.close();// free memory
    }

    public void play(boolean loop) {
        System.out.println("now playing audio");
        if (loop) {
            track.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            track.start();
        }
    }

    public void pause() {
        track.stop();
    }

    public void resume() {
        track.start();
    }

    void stop() {
        track.stop();
        track.setFramePosition(0);
    }

    public void update(long deltaTime) {

    }

    private Clip track;

    private String trackName;

    public void sync() {
        // TODO
    }

    public void close() {
        // TODO
    }

    public void incrementVolume() {
        // TODO
    }

    public void decrementVolume() {
        // TODO
    }

    public void setVolume(float v) {
        // TODO
    }
}