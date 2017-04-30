/*
 * Copyright (c) 2016-2017 Vladimir Schneider <vladimir.schneider@gmail.com>
 * Copyright 2000-2010 JetBrains s.r.o.
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
 */
package com.vladsch.MissingInActions.util.ui;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.ClickListener;
import com.intellij.ui.ColorChooser;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Vladimir Schneider
 *         Added Enabled property propagation to checkbox and repainting of color tab on color change
 *         Added unselected color property to display when checkbox is not selected
 *         Added update runnable to callback on color change
 * @author Konstantin Bulenkov
 */
public class CheckBoxWithColorChooser extends JPanel {
    private Color myColor;
    private final JCheckBox myCheckbox;
    private @Nullable Color myUnselectedColor;
    private Runnable myUpdateRunnable;
    private final JButton myColorButton;

    public CheckBoxWithColorChooser(String text, boolean selected, @NotNull Color color) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        myColor = color;
        myUnselectedColor = null;
        myCheckbox = new JCheckBox(text, selected);

        add(myCheckbox);
        myColorButton = new MyColorButton();
        add(myColorButton);
        myUpdateRunnable = null;

        myCheckbox.addActionListener((event) -> {
            myColorButton.repaint();
        });
    }

    public CheckBoxWithColorChooser(String text, boolean selected) {
        this(text, selected, Color.WHITE);
    }

    public CheckBoxWithColorChooser(String text) {
        this(text, false);
    }

    @Nullable
    public Color getUnselectedColor() {
        return myUnselectedColor;
    }

    public void setUnselectedColor(@Nullable final Color unselectedColor) {
        myUnselectedColor = unselectedColor;
        if (!myCheckbox.isSelected()) {
            myColorButton.repaint();
        }
    }

    public Runnable getUpdateRunnable() {
        return myUpdateRunnable;
    }

    public void setUpdateRunnable(final Runnable updateRunnable) {
        myUpdateRunnable = updateRunnable;
    }

    public void setMnemonic(char c) {
        myCheckbox.setMnemonic(c);
    }

    public Color getColor() {
        return myColor;
    }

    public void setColor(Color color) {
        myColor = color;
        myColorButton.repaint();
        if (myUpdateRunnable != null) {
            myUpdateRunnable.run();
        }
    }

    public void setSelected(boolean selected) {
        myCheckbox.setSelected(selected);
        myColorButton.repaint();
    }

    public boolean isSelected() {
        return myCheckbox.isSelected();
    }

    public void setEnabled(boolean enabled) {
        myCheckbox.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return myCheckbox.isEnabled();
    }

    private class MyColorButton extends JButton {
        MyColorButton() {
            setMargin(new Insets(0, 0, 0, 0));
            setDefaultCapable(false);
            setFocusable(false);
            if (SystemInfo.isMac) {
                putClientProperty("JButton.buttonType", "square");
            }
            new ClickListener() {
                @Override
                public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                    if (myCheckbox.isSelected()) {
                        final Color color = ColorChooser.chooseColor(myCheckbox, "Chose color", CheckBoxWithColorChooser.this.myColor);
                        if (color != null) {
                            myColor = color;
                            MyColorButton.this.repaint();
                            if (myUpdateRunnable != null) {
                                myUpdateRunnable.run();
                            }
                        }
                    }
                    return true;
                }
            }.installOn(this);
        }

        @Override
        public void paint(Graphics g) {
            final Color color = g.getColor();
            g.setColor(myCheckbox.isSelected() || myUnselectedColor == null ? myColor : myUnselectedColor);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(color);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            return new Dimension(JBUI.scale(12), JBUI.scale(12));
        }
    }
}
