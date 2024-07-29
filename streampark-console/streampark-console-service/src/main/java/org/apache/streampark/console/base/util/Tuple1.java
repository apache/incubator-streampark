/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.console.base.util;

import java.util.Objects;

public class Tuple1<T0> extends Tuple {

    private static final long serialVersionUID = 1L;

    /** Field 0 of the tuple. */
    public T0 t1;

    /** Creates a new tuple where all fields are null. */
    public Tuple1() {
    }

    /**
     * Creates a new tuple and assigns the given values to the tuple's fields.
     *
     * @param t0 The value for field 0
     */
    public Tuple1(T0 t0) {
        this.t1 = t0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int pos) {
        if (pos == 0) {
            return (T) this.t1;
        }
        throw new IndexOutOfBoundsException(String.valueOf(pos));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void set(T value, int pos) {
        if (pos == 0) {
            this.t1 = (T0) value;
        } else {
            throw new IndexOutOfBoundsException(String.valueOf(pos));
        }
    }

    /**
     * Sets new values to all fields of the tuple.
     *
     * @param f0 The value for field 0
     */
    public void set(T0 f0) {
        this.t1 = f0;
    }

    /**
     * Deep equality for tuples by calling equals() on the tuple members.
     *
     * @param o the object checked for equality
     * @return true if this is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tuple1)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        Tuple1 tuple = (Tuple1) o;
        return Objects.equals(t1, tuple.t1);
    }

    @Override
    public int hashCode() {
        return t1 != null ? t1.hashCode() : 0;
    }

    /**
     * Shallow tuple copy.
     *
     * @return A new Tuple with the same fields as this.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Tuple1<T0> copy() {
        return new Tuple1<>(this.t1);
    }

    /**
     * Creates a new tuple and assigns the given values to the tuple's fields. This is more convenient
     * than using the constructor, because the compiler can infer the generic type arguments
     * implicitly. For example: {@code Tuple3.of(n, x, s)} instead of {@code new Tuple3<Integer,
     * Double, String>(n, x, s)}
     */
    public static <T0> Tuple1<T0> of(T0 f0) {
        return new Tuple1<>(f0);
    }
}
