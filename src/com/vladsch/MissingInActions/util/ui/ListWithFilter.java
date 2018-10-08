/*
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

/*
 * @author max
 */
package com.vladsch.MissingInActions.util.ui;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.LightColors;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.UIBundle;
import com.intellij.ui.speedSearch.NameFilteringListModel;
import com.intellij.ui.speedSearch.SpeedSearch;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.Function;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.event.FocusEvent;

public class ListWithFilter<T> extends JPanel implements DataProvider {
  private final JList<? extends T> myList;
  private final SearchTextField mySearchField = new SearchTextField(false);
  private final NameFilteringListModel<T> myModel;
  private final JScrollPane myScrollPane;
  private final MySpeedSearch mySpeedSearch;
  private boolean myAutoPackHeight = true;

  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY.is(dataId)) {
      return mySearchField.getText();
    }
    return null;
  }

  @NotNull
  public static <T> ListWithFilter<T> wrap(@NotNull JList<? extends T> list, @NotNull JScrollPane scrollPane, @Nullable Function<? super T, String> namer) {
    return wrap(list, scrollPane, namer, false);
  }

  @NotNull
  public static <T> ListWithFilter<T> wrap(@NotNull JList<? extends T> list, @NotNull JScrollPane scrollPane, @Nullable Function<? super T, String> namer,
                                    boolean highlightAllOccurrences) {
    return new ListWithFilter<>(list, scrollPane, namer, highlightAllOccurrences);
  }

  private ListWithFilter(@NotNull JList<? extends T> list,
                         @NotNull JScrollPane scrollPane,
                         @Nullable Function<? super T, String> namer,
                         boolean highlightAllOccurrences) {
    super(new BorderLayout());

    if (list instanceof ComponentWithEmptyText) {
      ((ComponentWithEmptyText)list).getEmptyText().setText(UIBundle.message("message.noMatchesFound"));
    }

    myList = list;
    myScrollPane = scrollPane;

    mySearchField.getTextEditor().setFocusable(false);
    mySearchField.setVisible(false);

    add(mySearchField, BorderLayout.NORTH);
    add(myScrollPane, BorderLayout.CENTER);

    mySpeedSearch = new MySpeedSearch(highlightAllOccurrences);
    mySpeedSearch.setEnabled(namer != null);

    myList.addKeyListener(mySpeedSearch);
    int selectedIndex = myList.getSelectedIndex();
    int modelSize = myList.getModel().getSize();
    myModel = new NameFilteringListModel<T>(myList, namer, mySpeedSearch::shouldBeShowing, mySpeedSearch);
    if (myModel.getSize() == modelSize) {
      myList.setSelectedIndex(selectedIndex);
    }

    setBackground(list.getBackground());
    //setFocusable(true);
  }

  @Override
  protected void processFocusEvent(FocusEvent e) {
    super.processFocusEvent(e);
    if (e.getID() == FocusEvent.FOCUS_GAINED) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
        IdeFocusManager.getGlobalInstance().requestFocus(myList, true);
      });
    }
  }

  public boolean resetFilter() {
    boolean hadPattern = mySpeedSearch.isHoldingFilter();
    if (mySearchField.isVisible()) {
      mySpeedSearch.reset();
    }
    return hadPattern;
  }

  public SpeedSearch getSpeedSearch() {
    return mySpeedSearch;
  }

  private class MySpeedSearch extends SpeedSearch {
    boolean searchFieldShown;
    boolean myInUpdate;

    private MySpeedSearch(boolean highlightAllOccurrences) {
      super(highlightAllOccurrences);
      // native mac "clear button" is not captured by SearchTextField.onFieldCleared
      mySearchField.addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          if (myInUpdate) return;
          if (mySearchField.getText().isEmpty()) {
            mySpeedSearch.reset();
          }
        }
      });
      installSupplyTo(myList);
    }

    @Override
    public void update() {
      myInUpdate = true;
      mySearchField.getTextEditor().setBackground(UIUtil.getTextFieldBackground());
      onSpeedSearchPatternChanged();
      mySearchField.setText(getFilter());
      if (isHoldingFilter() && !searchFieldShown) {
        mySearchField.setVisible(true);
        searchFieldShown = true;
      }
      else if (!isHoldingFilter() && searchFieldShown) {
        mySearchField.setVisible(false);
        searchFieldShown = false;
      }

      myInUpdate = false;
      revalidate();
    }

    @Override
    public void noHits() {
      mySearchField.getTextEditor().setBackground(LightColors.RED);
    }

    private void revalidate() {
      JBPopup popup = PopupUtil.getPopupContainerFor(mySearchField);
      if (popup != null) {
        popup.pack(false, myAutoPackHeight);
      }
      ListWithFilter.this.revalidate();
    }
  }

  protected void onSpeedSearchPatternChanged() {
    T prevSelection = myList.getSelectedValue(); // save to restore the selection on filter drop
    myModel.refilter();
    if (myModel.getSize() > 0) {
      int fullMatchIndex = mySpeedSearch.isHoldingFilter() ? myModel.getClosestMatchIndex() : myModel.getElementIndex(prevSelection);
      if (fullMatchIndex != -1) {
        myList.setSelectedIndex(fullMatchIndex);
      }

      if (myModel.getSize() <= myList.getSelectedIndex() || !myModel.contains(myList.getSelectedValue())) {
        myList.setSelectedIndex(0);
      }
    }
    else {
      mySpeedSearch.noHits();
      revalidate();
    }
  }

  @NotNull
  public JList<? extends T> getList() {
    return myList;
  }

  public JScrollPane getScrollPane() {
    return myScrollPane;
  }

  public void setAutoPackHeight(boolean autoPackHeight) {
    myAutoPackHeight = autoPackHeight;
  }

  @Override
  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
      IdeFocusManager.getGlobalInstance().requestFocus(myList, true);
    });
  }
}
