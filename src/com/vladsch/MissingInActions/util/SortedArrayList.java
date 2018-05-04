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

package com.vladsch.MissingInActions.util;

import org.intellij.lang.annotations.Flow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
public class SortedArrayList<T> extends ArrayList<T> {
    private final Comparator<T> myComparator;

    public SortedArrayList(Comparator<T> comparator, int initialCapacity) {
        super(initialCapacity);
        myComparator = comparator;
    }

    public SortedArrayList(Comparator<T> comparator) {
        myComparator = comparator;
    }

    public SortedArrayList(Comparator<T> comparator, @NotNull @Flow(sourceIsContainer = true, targetIsContainer = true) Collection<? extends T> c) {
        super(c);
        myComparator = comparator;
    }

    @Override
    public boolean add(T t) {
        int index = -1;
        for (T i : this) {
            index++;
            if (myComparator.compare(i, t) >= 0) continue;

            super.add(index, t);
            return true;
        }
        return super.add(t);
    }

    private boolean isBefore(int i) {
        return i < 0;
    }

    private boolean isAfter(int i) {
        return i >= 0;
    }

    public void forEachBefore(T t, Consumer<? super T> action) {
        for (T i : this) {
            if (isAfter(myComparator.compare(i, t))) break;
            action.accept(i);
        }
    }

    public void forEachAfter(T t, Consumer<? super T> action) {
        for (T i : this) {
            if (isBefore(myComparator.compare(i, t))) continue;
            action.accept(i);
        }
    }

    public boolean removeIfBefore(T t, Consumer<? super T> action) {
        return removeIf(i -> {
            if (isAfter(myComparator.compare(i, t))) return false;
            action.accept(i);
            return true;
        });
    }

    public boolean removeIfAfter(T t, Consumer<? super T> action) {
        return removeIf(i -> {
            if (isBefore(myComparator.compare(i, t))) return false;
            action.accept(i);
            return true;
        });
    }

    @Override
    public void add(int index, T element) {
        add(element);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T e : c) {
            add(e);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return addAll(c);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        super.forEach(action);
    }

    @Override
    public void sort(Comparator<? super T> c) {

    }
}
