/*
 * Copyright (c) 2016-2018 Vladimir Schneider <vladimir.schneider@gmail.com>
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
 */

package com.vladsch.MissingInActions.util;

import com.intellij.ide.DataManager;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.vladsch.MissingInActions.manager.LineSelectionManager;
import com.vladsch.plugin.util.ui.CustomComponentAction;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.MissingResourceException;

public abstract class MiaComboBoxAction extends ComboBoxAction implements CustomComponentAction {
    private static final Key<Object> COMBO_BOX_EDITOR_PROPERTY_KEY = new Key<>("COMBO_BOX_EDITOR_PROPERTY");

    private static Icon myIcon = null;
    private static Icon myDisabledIcon = null;
    private static Icon myWin10ComboDropTriangleIcon = null;

    private boolean mySmallVariant = true;
    private String myPopupTitle;
    private boolean myShowNumbers;
    protected static boolean myPopupShowing = false;

    @SuppressWarnings("unused")
    @NotNull
    public static Icon getArrowIcon(boolean enabled) {
        if (UIUtil.isUnderWin10LookAndFeel()) {
            if (myWin10ComboDropTriangleIcon == null) {
                myWin10ComboDropTriangleIcon = IconLoader.getIcon("/com/intellij/ide/ui/laf/icons/win10/comboDropTriangle.png", MiaComboBoxAction.class);
            }
            return myWin10ComboDropTriangleIcon;
        }
        Icon icon = PluginIcons.Combo_arrow;
        if (myIcon != icon) {
            myIcon = icon;
            myDisabledIcon = IconLoader.getDisabledIcon(myIcon);
        }
        return enabled ? myIcon : myDisabledIcon;
    }

    protected MiaComboBoxAction() {
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = getEventEditor(e);

        Project project = e.getProject();
        if (editor == null || project == null) return;

        final JComponent button = e.getPresentation().getClientProperty(CUSTOM_COMPONENT_PROPERTY_KEY);
        final DataContext context = e.getDataContext();
        final DefaultActionGroup group = createPopupActionGroup(button, editor);
        final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                myPopupTitle, group, context, myShowNumbers, shouldShowDisabledActions(), false, null, getMaxRows(), getPreselectCondition());

        popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));

        popup.addListSelectionListener(ev -> actionSelected(editor, ev));

        popup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@NotNull final LightweightWindowEvent event) {
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                manager.setInSelectionStackPopup(true);
                popupStart(editor);
            }

            @Override
            public void onClosed(@NotNull final LightweightWindowEvent event) {
                popupDone(editor);
                LineSelectionManager manager = LineSelectionManager.getInstance(editor);
                manager.setInSelectionStackPopup(false);
                e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY, null);
            }
        });

        if (button instanceof MiaComboBoxButton && button.isShowing()) {
            popup.showUnderneathOf(button);
        } else {
            //popup.showInBestPositionFor(editor);
            popup.showInCenterOf(editor.getContentComponent());
        }
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation,  @NotNull String place) {
        JPanel panel = new JPanel(new GridBagLayout());
        MiaComboBoxButton button = createComboBoxButton(presentation);
        panel.add(button,
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insets(0, 3, 0, 3), 0, 0));
        return panel;
    }

    @NotNull
    protected MiaComboBoxButton createComboBoxButton(@NotNull Presentation presentation) {
        return new MiaComboBoxButton(presentation);
    }

    public boolean isSmallVariant() {
        return mySmallVariant;
    }

    public void setSmallVariant(boolean smallVariant) {
        mySmallVariant = smallVariant;
    }

    public void setPopupTitle(@NotNull String popupTitle) {
        myPopupTitle = popupTitle;
    }

    @Nullable
    protected static Editor getEventEditor(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            Object clientProperty = e.getPresentation().getClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY);
            if (clientProperty instanceof Editor) {
                editor = (Editor) clientProperty;
                if (editor.isDisposed()) {
                    e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY, null);
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
    public void update(@NotNull AnActionEvent e) {
        Editor editor = getEventEditor(e);
        e.getPresentation().putClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY, editor);
    }

    @SuppressWarnings("unused")
    public static boolean isPopupShowing() {
        return myPopupShowing;
    }

    protected boolean shouldShowDisabledActions() {
        return false;
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(final JComponent button) {
        Editor editor = (Editor) ((MiaComboBoxButton) button).myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY);
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

        private boolean isUnderGTKLookAndFeel() {
            return SystemInfoRt.isXWindow && UIManager.getLookAndFeel().getName().contains("GTK");
        }

        public MiaComboBoxButton(Presentation presentation) {
            super(presentation);

            myPresentation = presentation;
            setModel(new MyButtonModel());
            getModel().setEnabled(myPresentation.isEnabled());
            setVisible(presentation.isVisible());
            setHorizontalAlignment(LEFT);
            setFocusable(false);
            setOpaque(true);
            if (isSmallVariant()) {
                putClientProperty("styleCombo", MiaComboBoxAction.this);
            }
            setMargin(JBUI.insets(0, 5, 0, 2));
            if (isSmallVariant()) {
                setBorder(JBUI.Borders.empty(0, 2));
                if (!isUnderGTKLookAndFeel()) {
                    setFont(JBUI.Fonts.label(11));
                }
            }
        }

        @SuppressWarnings("unused")
        @NotNull
        private Runnable setForcePressed() {
            myForcePressed = true;
            repaint();

            return () -> {
                // give the button a chance to handle action listener
                ApplicationManager.getApplication().invokeLater(() -> {
                    myForcePressed = false;
                    repaint();
                }, ModalityState.any());
                repaint();
                fireStateChanged();
            };
        }

        @NotNull
        private ListPopup createActionPopup(@NotNull DataContext context, @NotNull JComponent component, @Nullable Runnable disposeCallback) {
            Editor editor = (Editor) myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY);
            DefaultActionGroup group = createPopupActionGroup(component, editor);
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    myPopupTitle, group, context, false, shouldShowDisabledActions(), false, disposeCallback, getMaxRows(), getPreselectCondition());
            popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));
            return popup;
        }

        @NotNull
        protected JBPopup createPopup(Runnable onDispose) {
            Editor editor = (Editor) myPresentation.getClientProperty(COMBO_BOX_EDITOR_PROPERTY_KEY);
            ListPopup popup = createActionPopup(getDataContext(), this, onDispose);

            popup.addListSelectionListener(e -> actionSelected(editor, e));

            popup.addListener(new JBPopupListener() {
                @Override
                public void beforeShown(@NotNull final LightweightWindowEvent event) {
                    popupStart(editor);
                }

                @Override
                public void onClosed(@NotNull final LightweightWindowEvent event) {
                    popupDone(editor);
                }
            });
            return popup;
        }

        @NotNull
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

        @Override
        public void updateUI() {
            super.updateUI();
            setMargin(JBUI.insets(0, 5, 0, 2));
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
                } else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
                    updateTooltipText((String) evt.getNewValue());
                } else if (Presentation.PROP_ICON.equals(propertyName)) {
                    setIcon((Icon) evt.getNewValue());
                } else if (Presentation.PROP_ENABLED.equals(propertyName)) {
                    setEnabled((Boolean) evt.getNewValue());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    protected boolean getShowNumbers() {
        return myShowNumbers;
    }

    protected void setShowNumbers(@SuppressWarnings("SameParameterValue") final boolean showNumbers) {
        myShowNumbers = showNumbers;
    }

    protected Condition<AnAction> getPreselectCondition() { return null; }
}
