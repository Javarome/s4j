/*
 * Copyright 2005 [org.ini4j] Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ini4j.addon;

import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

public class PreferencesWrapper extends Preferences {
    protected Preferences peer;

    public PreferencesWrapper(Preferences peer) {
        this.peer = peer;
    }

    public void put(String key, String value) {
        peer.put(key, value);
    }

    public String get(String key, String def) {
        return peer.get(key, def);
    }

    public void remove(String key) {
        peer.remove(key);
    }

    public void clear() throws BackingStoreException {
        peer.clear();
    }

    public void putInt(String key, int value) {
        peer.putInt(key, value);
    }

    public int getInt(String key, int def) {
        return peer.getInt(key, def);
    }

    public void putLong(String key, long value) {
        peer.putLong(key, value);
    }

    public long getLong(String key, long def) {
        return peer.getLong(key, def);
    }

    public void putBoolean(String key, boolean value) {
        peer.putBoolean(key, value);
    }

    public boolean getBoolean(String key, boolean def) {
        return peer.getBoolean(key, def);
    }

    public void putFloat(String key, float value) {
        peer.putFloat(key, value);
    }

    public float getFloat(String key, float def) {
        return peer.getFloat(key, def);
    }

    public void putDouble(String key, double value) {
        peer.putDouble(key, value);
    }

    public double getDouble(String key, double def) {
        return peer.getDouble(key, def);
    }

    public void putByteArray(String key, byte[] value) {
        peer.putByteArray(key, value);
    }

    public byte[] getByteArray(String key, byte[] def) {
        return peer.getByteArray(key, def);
    }

    public String[] keys() throws BackingStoreException {
        return peer.keys();
    }

    public String[] childrenNames() throws BackingStoreException {
        return peer.childrenNames();
    }

    public Preferences parent() {
        return peer.parent();
    }

    public Preferences node(String pathName) {
        return peer.node(pathName);
    }

    public boolean nodeExists(String pathName) throws BackingStoreException {
        return peer.nodeExists(pathName);
    }

    public void removeNode() throws BackingStoreException {
        peer.removeNode();
    }

    public String name() {
        return peer.name();
    }

    public String absolutePath() {
        return peer.absolutePath();
    }

    public boolean isUserNode() {
        return peer.isUserNode();
    }

    public String toString() {
        return peer.toString();
    }

    public void flush() throws BackingStoreException {
        peer.flush();
    }

    public void sync() throws BackingStoreException {
        peer.sync();
    }

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        peer.addPreferenceChangeListener(pcl);
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        peer.removePreferenceChangeListener(pcl);
    }

    public void addNodeChangeListener(NodeChangeListener ncl) {
        peer.addNodeChangeListener(ncl);
    }

    public void removeNodeChangeListener(NodeChangeListener ncl) {
        peer.removeNodeChangeListener(ncl);
    }

    public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        peer.exportNode(os);
    }

    public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        peer.exportSubtree(os);
    }
}
