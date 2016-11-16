/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
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

package com.vladsch.MissingInActions.util

open class RepeatedCharSequence(chars: CharArray, startIndex: Int, endIndex: Int) : CharSequence {
    val myChars: CharArray = chars
    val myStartIndex: Int = startIndex
    val myEndIndex: Int = endIndex

    override val length: Int get() = myEndIndex - myStartIndex

    constructor(char: Char, repeat: Int) : this(charArrayOf(char), 0, repeat)

    constructor(char: Char) : this(char, 1)

    constructor(text: String, repeat: Int) : this(text.toCharArray(), 0, text.length * repeat)

    constructor(text: String) : this(text, 1)

    override fun get(index: Int): Char {
        if (index >= 0 && index < length) return myChars[(myStartIndex + index) % myChars.size]
        throw IndexOutOfBoundsException()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex >= 0 && startIndex <= endIndex && endIndex <= length) return if (startIndex == endIndex) EMPTY_SEQUENCE else RepeatedCharSequence(myChars, myStartIndex + startIndex, myStartIndex + endIndex)
        throw IllegalArgumentException("subSequence($startIndex, $endIndex) in RepeatedCharSequence('', $myStartIndex, $myEndIndex)")
    }

    fun repeat(count:Int) : CharSequence {
        return RepeatedCharSequence(myChars, myStartIndex, (myEndIndex - myStartIndex)*count)
    }

    override fun toString(): String{
        return subSequence(0, length).toString()
    }
    
    companion object {
        @JvmField val EMPTY_SEQUENCE = RepeatedCharSequence(' ',0);
        @JvmField val EOL_SEQUENCE = RepeatedCharSequence('\n');
    }
}
