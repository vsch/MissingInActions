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

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Iterator;

public class ColorIterable implements Iterable<Color> {
    private final int myHueMinRaw;
    private final int myHueMaxRaw;
    private final int myHueSteps;
    private final int mySaturationMinRaw;
    private final int mySaturationMaxRaw;
    private final int mySaturationSteps;
    private final int myBrightnessMinRaw;
    private final int myBrightnessMaxRaw;
    private final int myBrightnessSteps;

    public ColorIterable(
            final int hueMinRaw,
            final int hueMaxRaw,
            final int hueSteps,
            final int saturationMinRaw,
            final int saturationMaxRaw,
            final int saturationSteps,
            final int brightnessMinRaw,
            final int brightnessMaxRaw,
            final int brightnessSteps
    ) {
        myHueMinRaw = hueMinRaw;
        myHueMaxRaw = hueMaxRaw;
        myHueSteps = UtilKt.minLimit(1, UtilKt.min(hueMaxRaw >= hueMinRaw ? hueMaxRaw - hueMinRaw : hueMinRaw - hueMaxRaw, hueSteps));
        mySaturationMinRaw = saturationMinRaw;
        mySaturationMaxRaw = saturationMaxRaw;
        mySaturationSteps = UtilKt.minLimit(1, UtilKt.min(saturationMaxRaw >= saturationMinRaw ? saturationMaxRaw - saturationMinRaw : saturationMinRaw - saturationMaxRaw, saturationSteps));
        myBrightnessMinRaw = brightnessMinRaw;
        myBrightnessMaxRaw = brightnessMaxRaw;
        myBrightnessSteps = UtilKt.minLimit(1, UtilKt.min(brightnessMaxRaw >= brightnessMinRaw ? brightnessMaxRaw - brightnessMinRaw : brightnessMinRaw - brightnessMaxRaw, brightnessSteps));
    }

    public int getMaxIndex() {
        int maxIndex = myHueSteps * mySaturationSteps * myBrightnessSteps;
        return maxIndex <= 1024 ? maxIndex : 1024;
    }

    @NotNull
    @Override
    public ColorIterator iterator() {
        return new ColorIterator(
                myHueMinRaw,
                myHueMaxRaw,
                myHueSteps,
                mySaturationMinRaw,
                mySaturationMaxRaw,
                mySaturationSteps,
                myBrightnessMinRaw,
                myBrightnessMaxRaw,
                myBrightnessSteps
        );
    }

    public class ColorIterator implements Iterator<Color> {
        private final int myHueMinRaw;
        private final int myHueMaxRaw;
        private final int myHueSteps;
        private final int mySaturationMinRaw;
        private final int mySaturationMaxRaw;
        private final int mySaturationSteps;
        private final int myBrightnessMinRaw;
        private final int myBrightnessMaxRaw;
        private final int myBrightnessSteps;
        private final int myMaxIndex;
        private int myIndex;
        private int myNextIndex;

        public ColorIterator(
                final int hueMinRaw,
                final int hueMaxRaw,
                final int hueSteps,
                final int saturationMinRaw,
                final int saturationMaxRaw,
                final int saturationSteps,
                final int brightnessMinRaw,
                final int brightnessMaxRaw,
                final int brightnessSteps
        ) {
            myHueMinRaw = hueMinRaw;
            myHueMaxRaw = hueMaxRaw;
            myHueSteps = hueSteps;
            mySaturationMinRaw = saturationMinRaw;
            mySaturationMaxRaw = saturationMaxRaw;
            mySaturationSteps = saturationSteps;
            myBrightnessMinRaw = brightnessMinRaw;
            myBrightnessMaxRaw = brightnessMaxRaw;
            myBrightnessSteps = brightnessSteps;
            int maxIndex = hueSteps * saturationSteps * brightnessSteps;
            myMaxIndex = maxIndex <= 1024 ? maxIndex : 1024;
            myIndex = 0;
            myNextIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return myNextIndex < myMaxIndex;
        }

        @Override
        public Color next() {
            if (myNextIndex >= myMaxIndex) throw new IllegalStateException("No more colors");

            myIndex = myNextIndex;
            myNextIndex++;

            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);

            float hue = (myHueMinRaw + ((myHueMaxRaw - myHueMinRaw) * hueIndex) / ((float) myHueSteps)) / 360.0f;
            float saturation = (mySaturationMinRaw + ((mySaturationMaxRaw - mySaturationMinRaw) * saturationIndex) / ((float) (mySaturationSteps > 1 ? mySaturationSteps - 1 : mySaturationSteps))) / 100.0f;
            float brightness = (myBrightnessMinRaw + ((myBrightnessMaxRaw - myBrightnessMinRaw) * brightnessIndex) / ((float) (myBrightnessSteps > 1 ? myBrightnessSteps - 1 : myBrightnessSteps))) / 100.0f;

            return Color.getHSBColor(hue, saturation, brightness);
        }

        public int getIndex() {
            return myIndex;
        }

        public int getMaxIndex() {
            return myMaxIndex;
        }

        public int getHueSteps() {
            return myHueSteps;
        }

        public int getSaturationSteps() {
            return mySaturationSteps;
        }

        public int getBrightnessSteps() {
            return myBrightnessSteps;
        }

        public int getHueIndex() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return hueIndex;
        }

        public boolean isHueStart() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return myIndex > 0 && hueIndex == 0;
        }

        public boolean isHueEnd() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return myHueSteps > 1 && hueIndex == myHueSteps - 1;
        }

        public int getSaturationIndex() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return saturationIndex;
        }

        public boolean isSaturationStart() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return myIndex > 0 && hueIndex == 0 && saturationIndex == 0;
        }

        public boolean isSaturationEnd() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return mySaturationSteps > 1 && hueIndex == 0 && saturationIndex == mySaturationSteps - 1;
        }

        public int getBrightnessIndex() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return brightnessIndex;
        }

        public boolean isBrightnessStart() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return myIndex > 0 && hueIndex == 0 && saturationIndex == 0 && brightnessIndex == 0;
        }

        public boolean isBrightnessEnd() {
            int i = myIndex;
            int hueIndex = (i % myHueSteps);
            i /= myHueSteps;
            int saturationIndex = (i % mySaturationSteps);
            i /= mySaturationSteps;
            int brightnessIndex = (i % myBrightnessSteps);
            return myBrightnessSteps > 1 && hueIndex == 0 && saturationIndex == 0 && brightnessIndex == myBrightnessSteps - 1;
        }
    }
}
