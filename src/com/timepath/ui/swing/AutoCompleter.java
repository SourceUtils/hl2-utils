package com.timepath.ui.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 *
 * @author TimePath
 */
public abstract class AutoCompleter {

    private static final Logger LOG = Logger.getLogger(AutoCompleter.class.getName());

    private int rowCount = 10;

    protected JList list = new JList() {
        {
            setFocusable(false);
            setRequestFocusEnabled(false);
        }
    };

    private JPopupMenu popup = new JPopupMenu() {
        {
            JScrollPane scroll = new JScrollPane(list) {
                {
                    setBorder(null);
                    getVerticalScrollBar().setFocusable(false);
                    getHorizontalScrollBar().setFocusable(false);
                }
            };
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            add(scroll);
        }
    };

    protected JTextComponent textComponent;

    private static final String COMPLETION = "AUTOCOMPLETION";

    public AutoCompleter(final JTextComponent jtc) {
        this.textComponent = jtc;
        jtc.putClientProperty(COMPLETION, this);

        jtc.getDocument().addDocumentListener(documentListener);

        jtc.registerKeyboardAction(shift(-1),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(1),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(-(rowCount - 1)),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(rowCount - 1),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(Integer.MIN_VALUE),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(Integer.MAX_VALUE),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(showAction,
                                   KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(hideAction,
                                   KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                   JComponent.WHEN_FOCUSED);
    }

    //<editor-fold defaultstate="collapsed" desc="Actions">
    private static Action acceptAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            completer.popup.setVisible(false);
            completer.acceptedListItem((String) completer.list.getSelectedValue());
        }
    };

    private static Action shift(final int val) {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JComponent tf = (JComponent) e.getSource();
                AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
                if(tf.isEnabled()) {
                    if(completer.popup.isVisible()) {
                        completer.shiftSelection(val);
                    }
                }
            }
        };
    }

    private static Action showAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            if(tf.isEnabled()) {
                if(!completer.popup.isVisible()) {
                    completer.showPopup();
                }
            }
        }
    };

    private static Action hideAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            if(tf.isEnabled()) {
                completer.popup.setVisible(false);
            }
        }
    };

    /**
     * Move the list selection within its boundaries
     */
    protected void shiftSelection(int val) {
        int si = list.getSelectedIndex() + val;
        si = Math.min(Math.max(0, si), list.getModel().getSize() - 1);
        list.setSelectedIndex(si);
        list.ensureIndexIsVisible(si);
    }
    //</editor-fold>

    private DocumentListener documentListener = new DocumentListener() {
        public void insertUpdate(DocumentEvent e) {
            showPopup();
        }

        public void removeUpdate(DocumentEvent e) {
            showPopup();
        }

        public void changedUpdate(DocumentEvent e) {
        }
    };

    private void showPopup() {
        popup.setVisible(false);
        if(textComponent.isEnabled() && updateListData() && list.getModel().getSize() != 0) {
            textComponent.getDocument().addDocumentListener(documentListener);
            textComponent.registerKeyboardAction(acceptAction,
                                                 KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                                                 JComponent.WHEN_FOCUSED);
            int size = list.getModel().getSize();
            list.setVisibleRowCount(size < rowCount ? size : rowCount);

            int xPos = 0;
            try {
                int pos = Math.min(textComponent.getCaret().getDot(),
                                   textComponent.getCaret().getMark());
                xPos = textComponent.getUI().modelToView(textComponent, pos).x;
            } catch(BadLocationException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            popup.show(textComponent, xPos, textComponent.getHeight());
        } else {
            popup.setVisible(false);
        }
        textComponent.requestFocus();
    }

    /**
     * update list model depending on the data in textfield
     * <p/>
     * @return whether to display the list
     */
    protected abstract boolean updateListData();

    /**
     * user has selected some item in the list, update textfield accordingly
     * <p/>
     * @param selected
     */
    protected abstract void acceptedListItem(String selected);

}