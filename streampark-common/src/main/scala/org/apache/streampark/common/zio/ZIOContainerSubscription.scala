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

package org.apache.streampark.common.zio

import org.apache.streampark.common.zio.ZIOExt.ZStreamOps

import zio.{durationInt, Chunk, Duration, Ref, Schedule, UIO}
import zio.concurrent.{ConcurrentMap, ConcurrentSet}
import zio.stream.{UStream, ZStream}

/** Subscription-ready data structure extension for ZIO Concurrent Collection. */
object ZIOContainerSubscription {

  private val defaultSubInterval: Duration = 500.millis

  implicit class ConcurrentSetExtension[E](set: ConcurrentSet[E]) {

    /*
     * Subscribe to the ConcurrentSet and get the diff of the set between each interval.
     * IN: [a, b, c], [a, b, c], [a, c]
     * OUT: [a, b, c], [a, c]
     */
    def subscribe(interval: Duration = defaultSubInterval): UStream[Set[E]] =
      ZStream
        .fromZIO(set.toSet)
        .repeat(Schedule.spaced(interval))
        .diffPrev

    /*
     * Subscribe to the ConcurrentSet and get the diff of the each flattened element.
     * IN: [a, b, c], [a, b, c], [a, c, e], [d]
     * OUT: a, b, c, e, d
     */
    def flatSubscribe(interval: Duration = defaultSubInterval): UStream[E] =
      ZStream
        .fromZIO(Ref.make(Set.empty[E]))
        .flatMap {
          prevSet =>
            subscribe(interval)
              .mapZIO(cur => prevSet.get.map(prev => (prev, cur)))
              .map { case (prev, cur) => cur -> cur.diff(prev) }
              .tap { case (cur, _) => prevSet.set(cur) }
              .flatMap { case (_, curDiff) => ZStream.fromIterable(curDiff) }
        }
  }

  implicit class ConcurrentMapExtension[K, V](map: ConcurrentMap[K, V]) {

    /*
     * Subscribe to the ConcurrentMap and get the diff of the set between each interval.
     * IN: [a -> 1, b -> 2], [a -> 1, b -> 2], [a -> 1, b -> 3]
     * OUT: [a -> 1, b -> 2], [a -> 1, b -> 3]
     */
    def subscribe(interval: Duration = 500.millis): UStream[Chunk[(K, V)]] =
      ZStream
        .fromZIO(map.toChunk)
        .repeat(Schedule.spaced(interval))
        .diffPrev

    /*
     * Subscribe to the ConcurrentMap and get the diff of the each flattened kv element.
     * IN: [a -> 1, b -> 2], [a -> 1, b -> 2, c -> 1], [a -> 1, b -> 3]
     * OUT: a -> 1, b -> 2, c -> 1, b -> 3
     */
    def flatSubscribe(interval: Duration = 500.millis): UStream[(K, V)] =
      ZStream
        .fromZIO(Ref.make(Chunk.empty[(K, V)]))
        .flatMap {
          prevMap =>
            subscribe(interval)
              .mapZIO(cur => prevMap.get.map(prev => (prev, cur)))
              .map { case (prev, cur) => cur -> cur.diff(prev) }
              .tap { case (cur, _) => prevMap.set(cur) }
              .flatMap { case (_, curDiff) => ZStream.fromIterable(curDiff) }
        }

    /*
     * Subscribe to the values of ConcurrentMap and get the diff of the each  kv element.
     * IN: [a -> a1, b -> b2], [a -> a1, b -> b2, c -> c1], [a -> a1, b -> b3]
     * OUT: [a1, b2], [c1], [b3]
     */
    def subscribeValues(interval: Duration = 500.millis): UStream[Chunk[V]] =
      subscribe(interval).map(_.map(_._2))

    /*
     * Subscribe to the values of ConcurrentMap and get the diff of the each fattened kv element.
     * IN: [a -> a1, b -> b2], [a -> a1, b -> b2, c -> c1], [a -> a1, b -> b3]
     * OUT: [a1, b2, c1, a1, b3]
     */
    def flatSubscribeValues(interval: Duration = 500.millis): UStream[V] =
      flatSubscribe(interval).map(_._2)

  }

  implicit class RefMapExtension[K, V](ref: Ref[Map[K, V]]) {

    /*
     * Subscribe to the Ref[Map] and get the diff of the set between each interval.
     * IN: [a -> 1, b -> 2], [a -> 1, b -> 2], [a -> 1, b -> 3]
     * OUT: [a -> 1, b -> 2], [a -> 1, b -> 3]
     */
    def subscribe(interval: Duration = defaultSubInterval): UStream[Chunk[(K, V)]] =
      ZStream
        .fromZIO(ref.get.map(m => Chunk.fromIterable(m)))
        .repeat(Schedule.spaced(interval))
        .diffPrev

    /*
     * Subscribe to the ConcurrentMap and get the diff of the each flattened kv element.
     * IN: [a -> 1, b -> 2], [a -> 1, b -> 2, c -> 1], [a -> 1, b -> 3]
     * OUT: a -> 1, b -> 2, c -> 1, b -> 3
     */
    def flatSubscribe(interval: Duration = defaultSubInterval) =
      ZStream
        .fromZIO(Ref.make(Chunk.empty[(K, V)]))
        .flatMap {
          prevMap =>
            subscribe(interval)
              .mapZIO(cur => prevMap.get.map(prev => (prev, cur)))
              .map { case (prev, cur) => cur -> cur.diff(prev) }
              .tap { case (cur, _) => prevMap.set(cur) }
              .flatMap { case (_, curDiff) => ZStream.fromIterable(curDiff) }
        }

    /*
     * Subscribe to the values of ConcurrentMap and get the diff of the each  kv element.
     * IN: [a -> a1, b -> b2], [a -> a1, b -> b2, c -> c1], [a -> a1, b -> b3]
     * OUT: [a1, b2], [c1], [b3]
     */
    def subscribeValues(interval: Duration = 500.millis): UStream[Chunk[V]] =
      subscribe(interval).map(_.map(_._2))

    /*
     * Subscribe to the values of ConcurrentMap and get the diff of the each fattened kv element.
     * IN: [a -> a1, b -> b2], [a -> a1, b -> b2, c -> c1], [a -> a1, b -> b3]
     * OUT: [a1, b2, c1, a1, b3]
     */
    def flatSubscribeValues(interval: Duration = 500.millis): UStream[V] =
      flatSubscribe(interval).map(_._2)

    def getValue(key: K): UIO[Option[V]] = ref.get.map(_.get(key))
  }

}
