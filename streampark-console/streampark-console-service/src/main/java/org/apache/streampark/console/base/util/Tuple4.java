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

/**
 * A tuple with 4 fields. Tuples are strongly typed; each field may be of a separate type. The
 * fields of the tuple can be accessed directly as public fields (f0, f1, ...) or via their position
 * through the {@link #get(int)} method. The tuple field positions start at zero.
 *
 * <p>Tuples are mutable types, meaning that their fields can be re-assigned. This allows functions
 * that work with Tuples to reuse objects in order to reduce pressure on the garbage collector.
 *
 * <p>Warning: If you subclass Tuple4, then be sure to either
 *
 * <ul>
 *   <li>not add any new fields, or
 *   <li>make it a POJO, and always declare the element type of your DataStreams/DataSets to your
 *       descendant type. (That is, if you have a "class Foo extends Tuple4", then don't use
 *       instances of Foo in a DataStream&lt;Tuple4&gt; / DataSet&lt;Tuple4&gt;, but declare it as
 *       DataStream&lt;Foo&gt; / DataSet&lt;Foo&gt;.)
 * </ul>
 *
 * @see Tuple
 * @param <T0> The type of field 0
 * @param <T1> The type of field 1
 * @param <T2> The type of field 2
 * @param <T3> The type of field 3
 */
public class Tuple4<T0, T1, T2, T3> extends Tuple {

  private static final long serialVersionUID = 1L;

  /** Field 0 of the tuple. */
  public T0 t1;
  /** Field 1 of the tuple. */
  public T1 t2;
  /** Field 2 of the tuple. */
  public T2 t3;
  /** Field 3 of the tuple. */
  public T3 t4;

  /** Creates a new tuple where all fields are null. */
  public Tuple4() {}

  /**
   * Creates a new tuple and assigns the given values to the tuple's fields.
   *
   * @param t0 The value for field 0
   * @param t1 The value for field 1
   * @param t2 The value for field 2
   * @param t4 The value for field 3
   */
  public Tuple4(T0 t0, T1 t1, T2 t2, T3 t4) {
    this.t1 = t0;
    this.t2 = t1;
    this.t3 = t2;
    this.t4 = t4;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(int pos) {
    switch (pos) {
      case 0:
        return (T) this.t1;
      case 1:
        return (T) this.t2;
      case 2:
        return (T) this.t3;
      case 3:
        return (T) this.t4;
      default:
        throw new IndexOutOfBoundsException(String.valueOf(pos));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> void set(T value, int pos) {
    switch (pos) {
      case 0:
        this.t1 = (T0) value;
        break;
      case 1:
        this.t2 = (T1) value;
        break;
      case 2:
        this.t3 = (T2) value;
        break;
      case 3:
        this.t4 = (T3) value;
        break;
      default:
        throw new IndexOutOfBoundsException(String.valueOf(pos));
    }
  }

  /**
   * Sets new values to all fields of the tuple.
   *
   * @param f0 The value for field 0
   * @param f1 The value for field 1
   * @param f2 The value for field 2
   * @param f3 The value for field 3
   */
  public void set(T0 f0, T1 f1, T2 f2, T3 f3) {
    this.t1 = f0;
    this.t2 = f1;
    this.t3 = f2;
    this.t4 = f3;
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
    if (!(o instanceof Tuple4)) {
      return false;
    }
    @SuppressWarnings("rawtypes")
    Tuple4 tuple = (Tuple4) o;
    if (!Objects.equals(t1, tuple.t1)) {
      return false;
    }
    if (!Objects.equals(t2, tuple.t2)) {
      return false;
    }
    if (!Objects.equals(t3, tuple.t3)) {
      return false;
    }
    return Objects.equals(t4, tuple.t4);
  }

  @Override
  public int hashCode() {
    int result = t1 != null ? t1.hashCode() : 0;
    result = 31 * result + (t2 != null ? t2.hashCode() : 0);
    result = 31 * result + (t3 != null ? t3.hashCode() : 0);
    result = 31 * result + (t4 != null ? t4.hashCode() : 0);
    return result;
  }

  /**
   * Shallow tuple copy.
   *
   * @return A new Tuple with the same fields as this.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Tuple4<T0, T1, T2, T3> copy() {
    return new Tuple4<>(this.t1, this.t2, this.t3, this.t4);
  }

  /**
   * Creates a new tuple and assigns the given values to the tuple's fields. This is more convenient
   * than using the constructor, because the compiler can infer the generic type arguments
   * implicitly. For example: {@code Tuple3.of(n, x, s)} instead of {@code new Tuple3<Integer,
   * Double, String>(n, x, s)}
   */
  public static <T0, T1, T2, T3> Tuple4<T0, T1, T2, T3> of(T0 f0, T1 f1, T2 f2, T3 f3) {
    return new Tuple4<>(f0, f1, f2, f3);
  }
}
