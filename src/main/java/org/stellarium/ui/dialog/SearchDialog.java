package org.stellarium.ui.dialog;

import org.stellarium.StelApp;
import org.stellarium.StelCore;
import org.stellarium.Translator;
import org.stellarium.data.ResourceLocatorUtil;
import org.stellarium.ui.StelUI;
import org.stellarium.ui.SwingUI;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * A dialog to search a stellar object by name.
 *
 * @author <a href="mailto:javarome@javarome.net">J&eacute;r&ocirc;me Beau</a>
 * @version 10 dec. 2006 23:43:22
 */
public class SearchDialog extends JDialog {
    private StelApp app;

    private JComboBox starEdit;

    private boolean isNotifying;

    public SearchDialog(JFrame owner, StelApp app) throws HeadlessException {
        super(owner, app.getCore().getTranslator().translate("ObjectSearch"));
        this.app = app;

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(5, 5, 0, 5);

        add(new JLabel(getTranslator().getString("SearchForEgSaturnPolarisHP6218OrionM31")), constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        ImageIcon searchp = new ImageIcon(ResourceLocatorUtil.getInstance().getTextureURL("bt_search.png"));
        SwingUI.scaleIcon(searchp, 32);
        JLabel psearch = new JLabel(searchp);
        add(psearch, constraints);

        final Action searchAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String objectName = (String) starEdit.getSelectedItem();
                if (objectName != null) {
                    getUi().gotoSearchedObject(objectName);
                    if (starEdit.isDisplayable()) {
                        starEdit.setPopupVisible(false);
                    }
                }
            }
        };

        constraints.gridx++;
        starEdit = new JComboBox();
        starEdit.setEditable(true);
        starEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchAction.actionPerformed(e);
            }
        });
        final JTextComponent editor = (JTextComponent) starEdit.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                Document document = editor.getDocument();
                try {
                    String objectName = document.getText(0, document.getEndPosition().getOffset()).trim();
                    getUi().autoCompleteSearchedObject(objectName);
                    starEdit.getEditor().setItem(objectName);
                    if (starEdit.isDisplayable()) {
                        starEdit.setPopupVisible(true);
                    }
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });

        add(starEdit, constraints);

        constraints.gridx++;
        JButton goButton = new JButton("GO");
        goButton.addActionListener(searchAction);
        add(goButton, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 3;

        add(new JLabel(""), constraints);
    }

    private Translator getTranslator() {
        return getCore().getTranslator();
    }

    private StelCore getCore() {
        return app.getCore();
    }

    private StelUI getUi() {
        return app.getUi();
    }

    public void setAutoCompleteOptions(List<String> strings) {
        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        for (String s : strings) {
            comboBoxModel.addElement(s);
        }
        starEdit.setModel(comboBoxModel);
    }
}
