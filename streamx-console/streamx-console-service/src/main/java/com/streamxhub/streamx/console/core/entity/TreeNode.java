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

import java.util.HashMap;
import java.util.Map;

class TreeNode {
    private final Character step;
    private boolean stop = false;
    private int cou = 0;
    private final Map<Character, TreeNode> next;

    /**
     * 根节点必须初始化为 ' ' , 否则只能有一个字符作为起始节点。
     *
     * @param step 当前节点的字符
     */
    public TreeNode(Character step) {
        this.step = step;
        this.next = new HashMap<>();
    }

    public void setCou(int cou) {
        this.cou += cou;
    }

    public void setStop() {
        this.stop = true;
    }

    public int getCou() {
        return cou;
    }

    public boolean isStop() {
        return stop;
    }

    public Character getStep() {
        return step;
    }

    public Map<Character, TreeNode> getNext() {
        return next;
    }
}
