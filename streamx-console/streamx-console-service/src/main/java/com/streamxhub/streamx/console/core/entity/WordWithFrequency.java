/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.entity;

/**
 * 用于维护一个以出现词频正序排列的 TreeSet 排序对象
 */
class WordWithFrequency implements Comparable<WordWithFrequency> {
    String word;
    Integer cou;

    public WordWithFrequency(String word, int cou) {
        this.word = word;
        this.cou = cou;
    }

    @Override
    public int compareTo(WordWithFrequency o) {
        int num = this.cou - o.cou;

        // * -1 是为了升序输出
        if (num == 0) {
            //这步非常关键，没有判定.计数相同名字不同 ，那set集合就默认是相同元素，就会被覆盖掉
            return this.word.compareTo(o.word) * -1;
        } else {
            return num * -1;
        }
    }
}
