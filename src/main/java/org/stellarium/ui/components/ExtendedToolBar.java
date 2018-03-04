/*
* This class is licensed under GNU public license.
* Original author: Marco Lopes (mlopes_filho@hotmail.com)
*/
package org.stellarium.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;

/**
 * Provides a JToolBar extension where it's possible to check wether the tool bar is
 * <p/>
 * floating or docked. Also, it provides a new property that can be listened by a
 * <p/>
 * PropertyChangeListener {@link #FLOATING}.
 * <p/>
 * <p/>
 * <p/>
 * In every HierarchyEvent where the parent is changed, a new property change event will be fired.
 * <p/>
 * Note that one event is fired in the begining telling that the tool bar is floating.
 * <p/>
 * <p/>
 * <p/>
 * This event is fired before the toolbar is processed and added to the parent and should be ignored.
 *
 * @author Last modified by $Author: MLopes $
 * @version $Revision: 1.4 $
 * @see java.beans.PropertyChangeListener
 */
public class ExtendedToolBar extends JToolBar {
    /**
     * the property fired to indicate a change in the status: floating(true) or docked(false)
     */
    public static final String FLOATING = "Floating";

    /**
     * keeps the original parent reference
     */
    private Component originalParent;

    /**
     * indicates whether the component is floating or not
     */
    private boolean floating = false;

    /**
     * @see javax.swing.JToolBar#JToolBar()
     */
    public ExtendedToolBar() {
        initComponents();
    }

    /**
     * @see javax.swing.JToolBar#JToolBar(int)
     */
    public ExtendedToolBar(final int orientation) {
        super(orientation);
        initComponents();
    }

    /**
     * @see javax.swing.JToolBar#JToolBar(String)
     */
    public ExtendedToolBar(final String name) {
        super(name);
        initComponents();
    }

    /**
     * @see javax.swing.JToolBar#JToolBar(String,int)
     */
    public ExtendedToolBar(final String name, final int orientation) {
        super(name, orientation);
        initComponents();
    }

    /**
     * returns true if this tool bar is floating.
     *
     * @return true if this tool bar is floating.
     */
    public boolean isFloating() {
        return floating;
    }

    /**
     * adds a HierarchyListener to listen to changes in its parent
     */
    private void initComponents() {
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                checkParent(e);
            }
        });
    }

    /**
     * checks if the parent has changed, means that the tool bar has been detached and
     * <p/>
     * it's now floating.
     * <p/>
     * The first event is used to get the original parent.
     * <p/>
     * In every event, a new property change event will be fired. Note that one event
     * <p/>
     * is fired in the begining telling that the tool bar is floating. This event is fired
     * <p/>
     * before the toolbar is processed and added to the parent and should be ignored.
     *
     * @param he the trigered hierarchy event
     */
    private void checkParent(final HierarchyEvent he) {
        if (originalParent == null) {
            originalParent = he.getChangedParent();
            return;
        }

        if (he.getChangeFlags() == HierarchyEvent.PARENT_CHANGED) {
            if (originalParent == he.getChangedParent()) {
                final boolean oldValue = floating;
                floating = false;
                firePropertyChange(FLOATING, oldValue, floating);
            } else {
                final boolean oldValue = floating;
                floating = true;
                firePropertyChange(FLOATING, oldValue, floating);
            }
        }
    }

    protected void processComponentKeyEvent(KeyEvent keyEvent) {
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(keyEvent);
        for (Component component : getComponents()) {
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                Action action = button.getAction();
                if (action.getValue(Action.ACCELERATOR_KEY) == keyStroke) {
                    action.actionPerformed(new ActionEvent(component, keyEvent.getID(), "pressed"));
                }
            }
        }
    }

    public void keyPressed(KeyEvent keyEvent) {
        processKeyEvent(keyEvent);
    }
}