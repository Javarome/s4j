package org.stellarium.ui;

import org.stellarium.ui.components.AKDockLayout;

import javax.swing.*;
import java.awt.*;

/**
 */
public class AKDockLayoutTest extends JFrame {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex) {
        }
        new AKDockLayoutTest();
    }


    public AKDockLayoutTest() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("AKDockLayout Test");
        setSize(240, 240);
        setLocationRelativeTo(null);

        Container c = getContentPane();
        c.setLayout(new AKDockLayout());

        JToolBar tbar = new JToolBar();
        tbar.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.black, 1),
                        tbar.getBorder()
                )
        );
        tbar.add(new JButton("one"));
        tbar.add(new JButton("two"));
        tbar.add(new JButton("three"));

        c.add(tbar, AKDockLayout.NORTH);


        tbar = new JToolBar();
        tbar.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.black, 1),
                        tbar.getBorder()
                )
        );
        tbar.add(new JButton("A"));
        tbar.add(new JButton("B"));
        tbar.add(new JButton("C"));
        tbar.add(new JButton("D"));
        tbar.add(new JButton("E"));
        tbar.add(new JButton("F"));

        c.add(tbar, AKDockLayout.NORTH);


        JPanel p = new JPanel();
        p.setBackground(Color.darkGray);
        p.setOpaque(true);
        c.add(p, AKDockLayout.CENTER);


        setVisible(true);
    }
}