package com.timepath.swing

import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
SuppressWarnings("serial")
abstract class AutoCompleter(val textComponent: JTextComponent) {
    val list: JList<String> = object : JList<String>() {
        {
            setFocusable(false)
            setRequestFocusEnabled(false)
        }
    }
    private val documentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) {
            showPopup()
        }

        override fun removeUpdate(e: DocumentEvent) {
            showPopup()
        }

        override fun changedUpdate(e: DocumentEvent) {
        }
    }
    val popup: JPopupMenu = object : JPopupMenu() {
        {
            val scroll = object : JScrollPane(list) {
                {
                    setBorder(null)
                    getVerticalScrollBar().setFocusable(false)
                    getHorizontalScrollBar().setFocusable(false)
                }
            }
            setBorder(BorderFactory.createLineBorder(Color.BLACK))
            add(scroll)
        }
    }
    public val rowCount: Int = 10

    {
        textComponent.putClientProperty(COMPLETION, this)
        textComponent.getDocument().addDocumentListener(documentListener)
        textComponent.registerKeyboardAction(shift(-1), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(shift(1), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(shift(-(rowCount - 1)), KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(shift(rowCount - 1), KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(shift(Integer.MIN_VALUE), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(shift(Integer.MAX_VALUE), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(showAction, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED)
        textComponent.registerKeyboardAction(hideAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED)
    }

    /**
     * Move the list selection within its boundaries
     *
     * @param val
     */
    fun shiftSelection(`val`: Int) {
        var si = list.getSelectedIndex() + `val`
        si = Math.min(Math.max(0, si), list.getModel().getSize() - 1)
        list.setSelectedIndex(si)
        list.ensureIndexIsVisible(si)
    }

    private fun showPopup() {
        popup.setVisible(false)
        if (textComponent.isEnabled() && updateListData() && (list.getModel().getSize() != 0)) {
            textComponent.getDocument().addDocumentListener(documentListener)
            textComponent.registerKeyboardAction(acceptAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED)
            val size = list.getModel().getSize()
            list.setVisibleRowCount(if ((size < rowCount)) size else rowCount)
            var xPos = 0
            try {
                val pos = Math.min(textComponent.getCaret().getDot(), textComponent.getCaret().getMark())
                xPos = textComponent.getUI().modelToView(textComponent, pos).x
            } catch (ex: BadLocationException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            popup.show(textComponent, xPos, textComponent.getHeight())
        } else {
            popup.setVisible(false)
        }
        textComponent.requestFocus()
    }

    /**
     * update list model depending on the data in textfield
     *
     * @return whether to display the list
     */
    protected abstract fun updateListData(): Boolean

    /**
     * user has selected some item in the list, update textfield accordingly
     *
     * @param selected
     */
    protected abstract fun acceptedListItem(selected: String)

    class object {

        private val COMPLETION = "AUTOCOMPLETION"
        private val acceptAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val tf = e.getSource() as JComponent
                val completer = tf.getClientProperty(COMPLETION) as AutoCompleter
                completer.popup.setVisible(false)
                completer.acceptedListItem(completer.list.getSelectedValue())
            }
        }
        private val hideAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val tf = e.getSource() as JComponent
                val completer = tf.getClientProperty(COMPLETION) as AutoCompleter
                if (tf.isEnabled()) {
                    completer.popup.setVisible(false)
                }
            }
        }
        private val showAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val tf = e.getSource() as JComponent
                val completer = tf.getClientProperty(COMPLETION) as AutoCompleter
                if (tf.isEnabled()) {
                    if (!completer.popup.isVisible()) {
                        completer.showPopup()
                    }
                }
            }
        }
        private val LOG = Logger.getLogger(javaClass<AutoCompleter>().getName())

        private fun shift(`val`: Int): ActionListener {
            return object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val tf = e.getSource() as JComponent
                    val completer = tf.getClientProperty(COMPLETION) as AutoCompleter
                    if (tf.isEnabled()) {
                        if (completer.popup.isVisible()) {
                            completer.shiftSelection(`val`)
                        }
                    }
                }
            }
        }
    }
}
