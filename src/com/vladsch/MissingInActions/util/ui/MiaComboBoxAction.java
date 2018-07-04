/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
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

package com.vladsch.MissingInActions.util.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.MissingResourceException;

public abstract class MiaComboBoxAction extends ComboBoxAction implements CustomComponentAction {
    //private static final Icon ARROW_ICON = UIUtil.isUnderDarcula() ? AllIcons.General.ComboArrow : AllIcons.General.ComboBoxButtonArrow;
    //private static final Icon DISABLED_ARROW_ICON = IconLoader.getDisabledIcon(ARROW_ICON);
    private static final String COMBO_BOX_EDITOR_PROPERTY = "COMBO_BOX_EDITOR_PROPERTY";
    private static Icon myIcon = null;
    private static Icon myDisabledIcon = null;
    private static Icon myWin10ComboDropTriangleIcon = null;

    private boolean mySmallVariant = true;
    private String myPopupTitle;
    private boolean myShowNumbers;
    protected static boolean myPopupShowing = false;

    public static Icon getArrowIcon(boolean enabled) {
        if (UIUtil.isUnderWin10LookAndFeel()) {
            if (myWin10ComboDropTriangleIcon == null) {
                myWin10ComboDropTriangleIcon = IconLoader.getIcon("/com/intellij/ide/ui/laf/icons/win10/comboDropTriangle.png");
            }
            return myWin10ComboDropTriangleIcon;
        }
        Icon icon = UIUtil.isUnderDarcula() ? AllIcons.General.ComboArrow : AllIcons.General.ComboBoxButtonArrow;
        if (myIcon != icon) {
            myIcon = icon;
            myDisabledIcon = IconLoader.getDisabledIcon(myIcon);
        }
        return enabled ? myIcon : myDisabledIcon;
    }

    protected MiaComboBoxAction() {
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = getEventEditor(e);

        Project project = e.getProject();
        if (editor == null || project == null) return;

        final JComponent button = (JComponent) e.getPresentation().getClientProperty(CUSTOM_COMPONENT_PROPERTY);
        final DataContext context = e.getDataContext();
        final DefaultActionGroup group = createPopupActionGroup(button, editor);
        final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                myPopupTitle, group, context, myShowNumbers, shouldShowDisabledActions(), false, null, getMaxRows(), getPreselectCondition());

        popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));

        popup.addListSelectionListener(ev -> {
            actionSelected(editor, ev);
        });

        popup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(final LightweightWindowEvent event) {
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                manager.setInSelectionStackPopup(true);
                popupStart(editor);
            }

            @Override
            public void onClosed(final LightweightWindowEvent event) {
                popupDone(editor);
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                manager.setInSelectionStackPopup(false);
                e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY, null);
            }
        });

        if (button instanceof MiaComboBoxButton && button.isShowing()) {
            popup.showUnderneathOf(button);
        } else {
            //popup.showInBestPositionFor(editor);
            popup.showInCenterOf(editor.getContentComponent());
        }
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        JPanel panel = new JPanel(new GridBagLayout());
        MiaComboBoxButton button = createComboBoxButton(presentation);
        panel.add(button,
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insets(0, 3, 0, 3), 0, 0));
        return panel;
    }

    protected MiaComboBoxButton createComboBoxButton(Presentation presentation) {
        return new MiaComboBoxButton(presentation);
    }

    public boolean isSmallVariant() {
        return mySmallVariant;
    }

    public void setSmallVariant(boolean smallVariant) {
        mySmallVariant = smallVariant;
    }

    public void setPopupTitle(String popupTitle) {
        myPopupTitle = popupTitle;
    }

    @Nullable
    protected static Editor getEventEditor(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            Object clientProperty = e.getPresentation().getClientProperty(COMBO_BOX_EDITOR_PROPERTY);
            if (clientProperty instanceof Editor) {
                editor = (Editor) clientProperty;
                if (editor.isDisposed()) {
                    e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY, null);
                }
            } else {
                Project project = e.getProject();
                if (project != null) {
                    FileEditorManager editorManager = FileEditorManager.getInstance(project);
                    editor = editorManager.getSelectedTextEditor();
                }
            }
        }
        return editor == null || editor.isDisposed() ? null : editor;
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = getEventEditor(e);
        e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY, editor);
    }

    public boolean isPopupShowing() {
        return myPopupShowing;
    }

    protected boolean shouldShowDisabledActions() {
        return false;
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
        Editor editor = (Editor) ((MiaComboBoxButton) button).myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY);
        return createPopupActionGroup(button, editor);
    }

    @NotNull
    protected abstract DefaultActionGroup createPopupActionGroup(@Nullable JComponent button, @Nullable Editor editor);

    protected int getMaxRows() {
        return 30;
    }

    protected int getMinHeight() {
        return 1;
    }

    protected int getMinWidth() {
        return 1;
    }

    protected void actionSelected(@Nullable Editor editor, ListSelectionEvent e) {

    }

    protected void popupStart(@Nullable Editor editor) {
        myPopupShowing = true;
    }

    protected void popupDone(@Nullable Editor editor) {
        myPopupShowing = false;
    }

    protected class MiaComboBoxButton extends ComboBoxAction.ComboBoxButton {
        final Presentation myPresentation;
        private boolean myForcePressed = false;
        private PropertyChangeListener myButtonSynchronizer;
        private boolean myMouseInside = false;
        private JBPopup myPopup;
        //private boolean myForceTransparent = false;

        public MiaComboBoxButton(Presentation presentation) {
            super(presentation);

            myPresentation = presentation;
            setModel(new MyButtonModel());
            getModel().setEnabled(myPresentation.isEnabled());
            setVisible(presentation.isVisible());
            setHorizontalAlignment(LEFT);
            setFocusable(false);
            setOpaque(true);
            //setForceTransparent(false);
            if (isSmallVariant()) {
                putClientProperty("styleCombo", MiaComboBoxAction.this);
            }
            Insets margins = getMargin();
            setMargin(JBUI.insets(0, 5, 0, 2));
            //setMargin(JBUI.insets(margins.top, 2, margins.bottom, 2));
            if (isSmallVariant()) {
                setBorder(JBUI.Borders.empty(0, 2));
                if (!UIUtil.isUnderGTKLookAndFeel()) {
                    setFont(JBUI.Fonts.label(11));
                }
            }

            //addActionListener(
            //        new ActionListener() {
            //            @Override
            //            public void actionPerformed(ActionEvent e) {
            //                if (!myForcePressed) {
            //                    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> showPopup());
            //                }
            //            }
            //        }
            //);
            //
            ////noinspection HardCodedStringLiteral
            //addMouseListener(new MouseAdapter() {
            //    @Override
            //    public void mouseEntered(MouseEvent e) {
            //        myMouseInside = true;
            //        repaint();
            //    }
            //
            //    @Override
            //    public void mouseExited(MouseEvent e) {
            //        myMouseInside = false;
            //        repaint();
            //    }
            //
            //    @Override
            //    public void mousePressed(final MouseEvent e) {
            //        if (SwingUtilities.isLeftMouseButton(e)) {
            //            e.consume();
            //            doClick();
            //        }
            //    }
            //
            //    @Override
            //    public void mouseReleased(MouseEvent e) {
            //        dispatchEventToPopup(e);
            //    }
            //});
            //addMouseMotionListener(new MouseMotionListener() {
            //    @Override
            //    public void mouseDragged(MouseEvent e) {
            //        mouseMoved(MouseEventAdapter.convert(e, e.getComponent(),
            //                MouseEvent.MOUSE_MOVED,
            //                e.getWhen(),
            //                e.getModifiers() | e.getModifiersEx(),
            //                e.getX(),
            //                e.getY()));
            //    }
            //
            //    @Override
            //    public void mouseMoved(MouseEvent e) {
            //        dispatchEventToPopup(e);
            //    }
            //});
        }

        //// Event forwarding. We need it if user does press-and-drag gesture for opening popup and choosing item there.
        //// It works in JComboBox, here we provide the same behavior
        //private void dispatchEventToPopup(MouseEvent e) {
        //    if (myPopup != null && myPopup.isVisible()) {
        //        JComponent content = myPopup.getContent();
        //        Rectangle rectangle = content.getBounds();
        //        Point location = rectangle.getLocation();
        //        SwingUtilities.convertPointToScreen(location, content);
        //        Point eventPoint = e.getLocationOnScreen();
        //        rectangle.setLocation(location);
        //        if (rectangle.contains(eventPoint)) {
        //            MouseEvent event = SwingUtilities.convertMouseEvent(e.getComponent(), e, myPopup.getContent());
        //            Component component = SwingUtilities.getDeepestComponentAt(content, event.getX(), event.getY());
        //            if (component != null)
        //                component.dispatchEvent(event);
        //        }
        //    }
        //}

        //public void setForceTransparent(boolean transparent) {
        //    myForceTransparent = transparent;
        //}

        @NotNull
        private Runnable setForcePressed() {
            myForcePressed = true;
            repaint();

            return () -> {
                // give the button a chance to handle action listener
                ApplicationManager.getApplication().invokeLater(() -> {
                    myForcePressed = false;
                    myPopup = null;
                    repaint();
                }, ModalityState.any());
                repaint();
                fireStateChanged();
            };
        }

        //@Nullable
        //@Override
        //public String getToolTipText() {
        //    return myForcePressed ? null : super.getToolTipText();
        //}
        //
        //public void showPopup() {
        //    JBPopup popup = createPopup(setForcePressed());
        //    if (Registry.is("ide.helptooltip.enabled")) {
        //        HelpTooltip.setMasterPopup(this, popup);
        //    }
        //
        //    popup.showUnderneathOf(this);
        //}
        //
        @NotNull
        private ListPopup createActionPopup(@NotNull DataContext context, @NotNull JComponent component, @Nullable Runnable disposeCallback) {
            Editor editor = (Editor) myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY);
            DefaultActionGroup group = createPopupActionGroup(component, editor);
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    myPopupTitle, group, context, false, shouldShowDisabledActions(), false, disposeCallback, getMaxRows(), getPreselectCondition());
            popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));
            return popup;
        }

        protected JBPopup createPopup(Runnable onDispose) {
            DataContext context = getDataContext();
            Editor editor = (Editor) myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY);
            //DefaultActionGroup group = createPopupActionGroup(this, editor);
            //ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            //        myPopupTitle, group, context, myShowNumbers, shouldShowDisabledActions(), false, onDispose, getMaxRows(), getPreselectCondition());
            //popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));
            ListPopup popup = createActionPopup(getDataContext(), this, onDispose);

            popup.addListSelectionListener(e -> {
                actionSelected(editor, e);
            });

            popup.addListener(new JBPopupListener() {
                @Override
                public void beforeShown(final LightweightWindowEvent event) {
                    popupStart(editor);
                }

                @Override
                public void onClosed(final LightweightWindowEvent event) {
                    popupDone(editor);
                }
            });
            return popup;
        }

        private MiaComboBoxAction getMyAction() {
            return MiaComboBoxAction.this;
        }

        protected DataContext getDataContext() {
            return DataManager.getInstance().getDataContext(this);
        }

        @Override
        public void removeNotify() {
            if (myButtonSynchronizer != null) {
                myPresentation.removePropertyChangeListener(myButtonSynchronizer);
                myButtonSynchronizer = null;
            }
            super.removeNotify();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (myButtonSynchronizer == null) {
                myButtonSynchronizer = new MyButtonSynchronizer();
                myPresentation.addPropertyChangeListener(myButtonSynchronizer);
            }
            initButton();
        }

        private void initButton() {
            setIcon(myPresentation.getIcon());
            setText(myPresentation.getText());
            updateTooltipText(myPresentation.getDescription());
            updateButtonSize();
        }

        private void updateTooltipText(String description) {
            String tooltip = KeymapUtil.createTooltipText(description, MiaComboBoxAction.this);
            try {
                if (Registry.is("ide.helptooltip.enabled") && StringUtil.isNotEmpty(tooltip)) {
                    HelpTooltip.dispose(this);
                    new HelpTooltip().setDescription(tooltip).setLocation(HelpTooltip.Alignment.BOTTOM).installOn(this);
                } else {
                    setToolTipText(!tooltip.isEmpty() ? tooltip : null);
                }
            } catch (MissingResourceException exception) {
                setToolTipText(!tooltip.isEmpty() ? tooltip : null);
            }
        }

        //@Override
        //public void updateUI() {
        //    super.updateUI();
        //    //if (!UIUtil.isUnderGTKLookAndFeel()) {
        //    //  setBorder(UIUtil.getButtonBorder());
        //    //}
        //    //((JComponent)getParent().getParent()).revalidate();
        //}

        @Override
        public void updateUI() {
            super.updateUI();
            setMargin(JBUI.insets(0, 5, 0, 2));
            updateButtonSize();
        }

        protected class MyButtonModel extends DefaultButtonModel {
            @Override
            public boolean isPressed() {
                return myForcePressed || super.isPressed();
            }

            @Override
            public boolean isArmed() {
                return myForcePressed || super.isArmed();
            }
        }

        private class MyButtonSynchronizer implements PropertyChangeListener {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (Presentation.PROP_TEXT.equals(propertyName)) {
                    setText((String) evt.getNewValue());
                    updateButtonSize();
                } else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
                    updateTooltipText((String) evt.getNewValue());
                } else if (Presentation.PROP_ICON.equals(propertyName)) {
                    setIcon((Icon) evt.getNewValue());
                    updateButtonSize();
                } else if (Presentation.PROP_ENABLED.equals(propertyName)) {
                    setEnabled(((Boolean) evt.getNewValue()).booleanValue());
                }
            }
        }

        //@Override
        //public Insets getInsets() {
        //    final Insets insets = super.getInsets();
        //    insets.right += getArrowIcon(isEnabled()).getIconWidth();
        //    return insets;
        //}
        //
        //@Override
        //public Insets getInsets(Insets insets) {
        //    final Insets result = super.getInsets(insets);
        //    result.right += getArrowIcon(isEnabled()).getIconWidth();
        //    return result;
        //}
        //
        //@Override
        //public boolean isOpaque() {
        //    return !isSmallVariant();
        //}

        //protected Icon getArrowIcon() {
        //    if (UIUtil.isUnderWin10LookAndFeel()) {
        //        return IconLoader.getIcon("/com/intellij/ide/ui/laf/icons/win10/comboDropTriangle.png");
        //    }
        //    return isEnabled() ? ARROW_ICON : DISABLED_ARROW_ICON;
        //}

        //@Override
        //public Dimension getPreferredSize() {
        //    Dimension prefSize = super.getPreferredSize();
        //    int width = prefSize.width + getArrowIcon(isEnabled()).getIconWidth()
        //            + (StringUtil.isNotEmpty(getText()) ? getIconTextGap() : 0)
        //            + (UIUtil.isUnderWin10LookAndFeel() ? JBUI.scale(6) : 0);
        //
        //    Dimension size = new Dimension(width, isSmallVariant() ? JBUI.scale(24) : Math.max(JBUI.scale(24), prefSize.height));
        //    JBInsets.addTo(size, getMargin());
        //    return size;
        //}
        //
        //@Override
        //public Dimension getMinimumSize() {
        //    return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
        //}
        //
        //@Override
        //public Font getFont() {
        //    return SystemInfo.isMac && isSmallVariant() ? UIUtil.getLabelFont(UIUtil.FontSize.SMALL) : UIUtil.getLabelFont();
        //}
        //
        //
        //@Override
        //protected Graphics getComponentGraphics(Graphics graphics) {
        //    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
        //}

        //@Override
        //public void paint(Graphics g) {
        //        super.paint(g);
        //final boolean isEmpty = getIcon() == null && StringUtil.isEmpty(getText());
        //final Dimension size = getSize();
        //
        //if (SystemInfo.isMac && UIUtil.isUnderIntelliJLaF()) {
        //    super.paint(g);
        //} else {
        //    UISettings.setupAntialiasing(g);
        //
        //    final Color textColor = isEnabled()
        //            ? UIManager.getColor("Panel.foreground")
        //            : UIUtil.getInactiveTextColor();
        //    if (myForceTransparent) {
        //        final Icon icon = getIcon();
        //        int x = 7;
        //        if (icon != null) {
        //            icon.paintIcon(this, g, x, (size.height - icon.getIconHeight()) / 2);
        //            x += icon.getIconWidth() + 3;
        //        }
        //        if (!StringUtil.isEmpty(getText())) {
        //            final Font font = getFont();
        //            g.setFont(font);
        //            g.setColor(textColor);
        //            UIUtil.drawCenteredString((Graphics2D) g, new Rectangle(x, 0, Integer.MAX_VALUE, size.height), getText(), false, true);
        //        }
        //    } else {
        //
        //        if (isSmallVariant()) {
        //            final Graphics2D g2 = (Graphics2D) g;
        //            g2.setColor(UIUtil.getControlColor());
        //            final int w = getWidth();
        //            final int h = getHeight();
        //            if (getModel().isArmed() && getModel().isPressed()) {
        //                g2.setPaint(UIUtil.getGradientPaint(0, 0, UIUtil.getControlColor(), 0, h, ColorUtil.shift(UIUtil.getControlColor(), 0.8)));
        //            } else {
        //                if (UIUtil.isUnderDarcula()) {
        //                    g2.setPaint(UIUtil.getGradientPaint(0, 0, ColorUtil.shift(UIUtil.getControlColor(), 1.1), 0, h,
        //                            ColorUtil.shift(UIUtil.getControlColor(), 0.9)));
        //                } else {
        //                    g2.setPaint(UIUtil.getGradientPaint(0, 0, new JBColor(SystemInfo.isMac ? Gray._226 : Gray._245, Gray._131), 0, h,
        //                            new JBColor(SystemInfo.isMac ? Gray._198 : Gray._208, Gray._128)));
        //                }
        //            }
        //            if (UIUtil.isUnderWin10LookAndFeel()) {
        //                g2.setColor(getBackground());
        //                g2.fillRect(2, 0, w - 2, h);
        //            } else {
        //                g2.fillRoundRect(2, 0, w - 2, h, 5, 5);
        //            }
        //
        //            Color borderColor = myMouseInside ? new JBColor(Gray._111, Gray._118) : new JBColor(Gray._151, Gray._95);
        //            g2.setPaint(borderColor);
        //            if (UIUtil.isUnderWin10LookAndFeel()) {
        //                g2.setColor(myMouseInside ? Gray.x96 : Gray.xAD);
        //                g2.drawRect(2, 0, w - 3, h - 1);
        //            } else {
        //                g2.drawRoundRect(2, 0, w - 3, h - 1, 5, 5);
        //            }
        //
        //            final Icon icon = getIcon();
        //            int x = 7;
        //            if (icon != null) {
        //                icon.paintIcon(this, g, x, (size.height - icon.getIconHeight()) / 2);
        //                x += icon.getIconWidth() + 3;
        //            }
        //            if (!StringUtil.isEmpty(getText())) {
        //                final Font font = getFont();
        //                g2.setFont(font);
        //                g2.setColor(textColor);
        //                UIUtil.drawCenteredString(g2, new Rectangle(x, 0, Integer.MAX_VALUE, size.height), getText(), false, true);
        //            }
        //        } else {
        //            super.paint(g);
        //        }
        //    }
        //}
        //final Insets insets = super.getInsets();
        //final Icon icon = getArrowIcon(isEnabled());
        //int x;
        //if (isEmpty) {
        //    x = (size.width - icon.getIconWidth()) / 2;
        //} else {
        //    if (isSmallVariant()) {
        //        x = size.width - icon.getIconWidth() - insets.right + 1;
        //        if (SystemInfo.isMac && UIUtil.isUnderIntelliJLaF()) {
        //            x -= 3;
        //        } else if (UIUtil.isUnderWin10LookAndFeel()) {
        //            x -= JBUI.scale(3);
        //        }
        //    } else {
        //        x = size.width - icon.getIconWidth() - insets.right + (UIUtil.isUnderNimbusLookAndFeel() ? -3 : 2);
        //    }
        //}
        //
        //icon.paintIcon(null, g, x, (size.height - icon.getIconHeight()) / 2);
        //g.setPaintMode();
        //}
        //
        //protected void updateButtonSize() {
        //    invalidate();
        //    repaint();
        //    setSize(getPreferredSize());
        //    repaint();
        //}
    }

    protected boolean getShowNumbers() {
        return myShowNumbers;
    }

    protected void setShowNumbers(final boolean showNumbers) {
        myShowNumbers = showNumbers;
    }

    protected Condition<AnAction> getPreselectCondition() { return null; }
}
