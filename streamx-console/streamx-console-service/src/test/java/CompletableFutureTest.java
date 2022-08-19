/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.streamxhub.streamx.common.util.CompletableFutureUtils;

import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CompletableFutureTest {

    @Test
    public void test() throws Exception {

        //启动任务需要10秒...
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> runStart(10));

        // 5秒之后停止任务.
        CompletableFuture.runAsync(() -> runStop(future, 5));

        //设置启动超时时间为20秒
        CompletableFutureUtils.runTimeout(
            future,
            20L,
            TimeUnit.SECONDS,
            r -> System.out.println(r),
            e -> {
                if (e.getCause() instanceof CancellationException) {
                    System.out.println("任务被终止....");
                } else {
                    e.printStackTrace();
                    future.cancel(true);
                    System.out.println("start timeout");
                }
            }
        ).get();
        if (future.isCancelled()) {
            System.out.println("cancelled...");
        }
    }

    private void runStop(CompletableFuture<String> future, int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!future.isCancelled()) {
            System.out.println("强行停止任务");
            future.cancel(true);
        } else {
            System.out.println("任务正常停止...");
        }
    }

    /**
     * 模拟启动需要30秒钟.
     *
     * @return
     */
    private String runStart(int sec) {
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "start successful";
    }
}
