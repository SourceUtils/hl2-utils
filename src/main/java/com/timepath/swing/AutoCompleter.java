package com.timepath.swing;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
abstract class AutoCompleter {

    private static final String         COMPLETION   = "AUTOCOMPLETION";
    private static final ActionListener acceptAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            completer.getPopup().setVisible(false);
            completer.acceptedListItem(completer.getList().getSelectedValue());
        }
    };
    private static final ActionListener hideAction   = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            if(tf.isEnabled()) {
                completer.getPopup().setVisible(false);
            }
        }
    };
    private static final ActionListener showAction   = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent tf = (JComponent) e.getSource();
            AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
            if(tf.isEnabled()) {
                if(!completer.getPopup().isVisible()) {
                    completer.showPopup();
                }
            }
        }
    };
    private static final Logger         LOG          = Logger.getLogger(AutoCompleter.class.getName());
    final                JList<String>  list         = new JList<String>() {
        {
            setFocusable(false);
            setRequestFocusEnabled(false);
        }
    };
    final JTextComponent textComponent;
    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            showPopup();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            showPopup();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
    };
    private final JPopupMenu       popup            = new JPopupMenu() {
        {
            JScrollPane scroll = new JScrollPane(getList()) {
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
    private final int              rowCount         = 10;

    AutoCompleter(JTextComponent jtc) {
        textComponent = jtc;
        jtc.putClientProperty(COMPLETION, this);
        jtc.getDocument().addDocumentListener(documentListener);
        jtc.registerKeyboardAction(shift(-1), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(1), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(-( rowCount - 1 )),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(rowCount - 1),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(Integer.MIN_VALUE),
                                   KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(shift(Integer.MAX_VALUE), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(showAction,
                                   KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK),
                                   JComponent.WHEN_FOCUSED);
        jtc.registerKeyboardAction(hideAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
    }

    private static ActionListener shift(final int val) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComponent tf = (JComponent) e.getSource();
                AutoCompleter completer = (AutoCompleter) tf.getClientProperty(COMPLETION);
                if(tf.isEnabled()) {
                    if(completer.getPopup().isVisible()) {
                        completer.shiftSelection(val);
                    }
                }
            }
        };
    }

    /**
     * @return the popup
     */
    JPopupMenu getPopup() {
        return popup;
    }

    /**
     * Move the list selection within its boundaries
     *
     * @param val
     */
    void shiftSelection(int val) {
        int si = list.getSelectedIndex() + val;
        si = Math.min(Math.max(0, si), list.getModel().getSize() - 1);
        list.setSelectedIndex(si);
        list.ensureIndexIsVisible(si);
    }

    public int getRowCount() {
        return rowCount;
    }

    /**
     * @return the list
     */
    JList<String> getList() {
        return list;
    }

    private void showPopup() {
        popup.setVisible(false);
        if(textComponent.isEnabled() && updateListData() && ( list.getModel().getSize() != 0 )) {
            textComponent.getDocument().addDocumentListener(documentListener);
            textComponent.registerKeyboardAction(acceptAction,
                                                 KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                                                 JComponent.WHEN_FOCUSED);
            int size = list.getModel().getSize();
            list.setVisibleRowCount(( size < rowCount ) ? size : rowCount);
            int xPos = 0;
            try {
                int pos = Math.min(textComponent.getCaret().getDot(), textComponent.getCaret().getMark());
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
     *
     * @return whether to display the list
     */
    protected abstract boolean updateListData();

    /**
     * user has selected some item in the list, update textfield accordingly
     *
     * @param selected
     */
    protected abstract void acceptedListItem(String selected);
}
