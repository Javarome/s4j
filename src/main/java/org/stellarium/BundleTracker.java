/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.stellarium;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a very simple bundle tracker utility class that tracks active
 * bundles. The tracker must be given a bundle context upon creation,
 * which it uses to listen for bundle events. The bundle tracker must be
 * opened to track objects and closed when it is no longer needed. This
 * class is abstract, which means in order to use it you must create a
 * subclass of it. Subclasses must implement the <tt>addedBundle()</tt>
 * and <tt>removedBundle()</tt> methods, which can be used to perform some
 * custom action upon the activation or deactivation of bundles. Since this
 * tracker is quite simple, its concurrency control approach is also
 * simplistic. This means that subclasses should take great care to ensure
 * that their <tt>addedBundle()</tt> and <tt>removedBundle()</tt> methods
 * are very simple and do not do anything to change the state of any bundles.
 */
public abstract class BundleTracker {
    final Set<Bundle> bundleSet = new HashSet<Bundle>();
    final BundleContext context;
    final SynchronousBundleListener listener;
    boolean open;

    /**
     * Constructs a bundle tracker object that will use the specified
     * bundle context.
     *
     * @param context The bundle context to use to track bundles.
     */
    public BundleTracker(BundleContext context) {
        this.context = context;
        listener = new SynchronousBundleListener() {
            public void bundleChanged(BundleEvent evt) {
                synchronized (BundleTracker.this) {
                    if (!open) {
                        return;
                    }

                    if (evt.getType() == BundleEvent.STARTED) {
                        if (!bundleSet.contains(evt.getBundle())) {
                            bundleSet.add(evt.getBundle());
                            addedBundle(evt.getBundle());
                        }
                    } else if (evt.getType() == BundleEvent.STOPPED) {
                        if (bundleSet.contains(evt.getBundle())) {
                            bundleSet.remove(evt.getBundle());
                            removedBundle(evt.getBundle());
                        }
                    }
                }
            }
        };
    }

    /**
     * Returns the current set of active bundles.
     *
     * @return The current set of active bundles.
     */
    public synchronized Bundle[] getBundles() {
        return bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    /**
     * Call this method to start the tracking of active bundles.
     */
    public synchronized void open() {
        if (!open) {
            open = true;

            context.addBundleListener(listener);

            Bundle[] bundles = context.getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    bundleSet.add(bundle);
                    addedBundle(bundle);
                }
            }
        }
    }

    /**
     * Call this method to stop the tracking of active bundles.
     */
    public synchronized void close() {
        if (open) {
            open = false;

            context.removeBundleListener(listener);

            Bundle[] bundles = bundleSet.toArray(new Bundle[bundleSet.size()]);
            for (Bundle bundle : bundles) {
                if (bundleSet.remove(bundle)) {
                    removedBundle(bundle);
                }
            }
        }
    }

    /**
     * Subclasses must implement this method; it can be used to perform
     * actions upon the activation of a bundle. Subclasses should keep
     * this method implementation as simple as possible and should not
     * cause the change in any bundle state to avoid concurrency issues.
     *
     * @param bundle The bundle being added to the active set.
     */
    protected abstract void addedBundle(Bundle bundle);

    /**
     * Subclasses must implement this method; it can be used to perform
     * actions upon the deactivation of a bundle. Subclasses should keep
     * this method implementation as simple as possible and should not
     * cause the change in any bundle state to avoid concurrency issues.
     *
     * @param bundle The bundle being removed from the active set.
     */
    protected abstract void removedBundle(Bundle bundle);
}