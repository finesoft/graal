/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.espresso.nodes.bytecodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.espresso.meta.Meta;
import com.oracle.truffle.espresso.nodes.EspressoNode;
import com.oracle.truffle.espresso.nodes.quick.interop.ForeignArrayUtils;
import com.oracle.truffle.espresso.runtime.staticobject.StaticObject;

/**
 * LALOAD bytecode with interop extensions.
 *
 * <h3>Extensions</h3>
 * <ul>
 * <li>For {@link InteropLibrary#hasArrayElements(Object) array-like} foreign objects, LALOAD is
 * mapped to {@link InteropLibrary#readArrayElement(Object, long)}.</li>
 * </ul>
 *
 * <h3>Exceptions</h3>
 * <ul>
 * <li>Throws guest {@link ArrayIndexOutOfBoundsException} if the index is out of bounds.</li>
 * <li>Throws guest {@link ClassCastException} if the result cannot be converted to long.</li>
 * </ul>
 */
@GenerateUncached
@NodeInfo(shortName = "LALOAD")
public abstract class LongArrayLoad extends EspressoNode {

    public abstract long execute(StaticObject receiver, int index);

    @Specialization
    long doWithNullCheck(StaticObject array, int index,
                    @Cached NullCheck nullCheck,
                    @Cached WithoutNullCheck longArrayLoad) {
        return longArrayLoad.execute(nullCheck.execute(array), index);
    }

    @GenerateUncached
    @NodeInfo(shortName = "LALOAD !nullcheck")
    public abstract static class WithoutNullCheck extends EspressoNode {

        protected static final int LIMIT = 2;

        public abstract long execute(StaticObject receiver, int index);

        @Specialization(guards = "array.isEspressoObject()")
        long doEspresso(StaticObject array, int index) {
            assert !StaticObject.isNull(array);
            return getContext().getInterpreterToVM().getArrayLong(getLanguage(), index, array);
        }

        @Specialization(guards = "array.isForeignObject()")
        long doArrayLike(StaticObject array, int index,
                        @CachedLibrary(limit = "LIMIT") InteropLibrary arrayInterop,
                        @CachedLibrary(limit = "LIMIT") InteropLibrary elemInterop,
                        @Cached BranchProfile exceptionProfile) {
            assert !StaticObject.isNull(array);
            Meta meta = getContext().getMeta();
            Object result = ForeignArrayUtils.readForeignArrayElement(array, index, getLanguage(), meta, arrayInterop, exceptionProfile);
            try {
                return elemInterop.asLong(result);
            } catch (UnsupportedMessageException e) {
                exceptionProfile.enter();
                throw meta.throwExceptionWithMessage(meta.java_lang_ClassCastException, "Could not cast the foreign array element to long");
            }
        }
    }
}
