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

package org.apache.streampark.console.core.task;

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Savepoint;
import org.apache.streampark.console.core.enums.CheckPointStatus;
import org.apache.streampark.console.core.enums.FailoverStrategy;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.SavepointService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CheckpointProcessor {

  private static final Byte DEFAULT_FLAG_BYTE = Byte.valueOf("0");
  private static final Integer SAVEPOINT_CACHE_HOUR = 1;

  private final Cache<String, Long> checkPointCache =
      Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build();

  /**
   * Cache to store the savepoint if be stored in the db. Use the {appId}_{jobID}_{chkId} from
   * {@link CheckPointKey#getSavePointId()} as the cache key to save the trace of the savepoint. And
   * try best to make sure the every savepoint would be stored into DB. Especially for the case
   * 'maxConcurrent of Checkpoint' > 1: 1. savepoint(n-1) is completed after completed
   * checkpoint(n); 2. savepoint(n-1) is completed after completed savepoint(n).
   */
  private final Cache<String, Byte> savepointedCache =
      Caffeine.newBuilder().expireAfterWrite(SAVEPOINT_CACHE_HOUR, TimeUnit.HOURS).build();

  private final Map<Long, Counter> checkPointFailedCache = new ConcurrentHashMap<>(0);

  @Autowired private ApplicationService applicationService;

  @Autowired private AlertService alertService;

  @Autowired private SavepointService savepointService;

  @Autowired private FlinkAppHttpWatcher flinkAppHttpWatcher;

  public void process(Application application, @Nonnull CheckPoints checkPoints) {
    checkPoints.getLatestCheckpoint().forEach(checkPoint -> process(application, checkPoint));
  }

  private void process(Application application, @Nonnull CheckPoints.CheckPoint checkPoint) {
    String jobID = application.getJobId();
    Long appId = application.getId();
    CheckPointStatus status = checkPoint.getCheckPointStatus();
    CheckPointKey checkPointKey = new CheckPointKey(appId, jobID, checkPoint.getId());

    if (CheckPointStatus.COMPLETED.equals(status)) {
      switch (checkPoint.getCheckPointType()) {
        case SAVEPOINT:
          if (checkSaveForSavepoint(checkPointKey, checkPoint)) {
            savepointedCache.put(checkPointKey.getSavePointId(), DEFAULT_FLAG_BYTE);
            saveSavepoint(checkPoint, application.getId());
            flinkAppHttpWatcher.cleanSavepoint(application);
            return;
          }
          break;
        case CHECKPOINT:
          Long latestChkId = getLatestCheckpointId(appId, checkPointKey.getCheckPointId());
          if (checkSaveForCheckpoint(checkPoint, latestChkId)) {
            checkPointCache.put(checkPointKey.getCheckPointId(), checkPoint.getId());
            saveSavepoint(checkPoint, application.getId());
          }
          break;
        default:
          break;
      }
      return;
    }

    if (shouldProcessFailedTrigger(checkPoint, application.cpFailedTrigger(), status)) {
      Counter counter = checkPointFailedCache.get(appId);
      if (counter == null) {
        checkPointFailedCache.put(appId, new Counter(checkPoint.getTriggerTimestamp()));
      } else {
        long minute = counter.getDuration(checkPoint.getTriggerTimestamp());
        if (minute <= application.getCpFailureRateInterval()
            && counter.getCount() >= application.getCpMaxFailureInterval()) {
          checkPointFailedCache.remove(appId);
          FailoverStrategy failoverStrategy = FailoverStrategy.of(application.getCpFailureAction());
          if (failoverStrategy == null) {
            throw new IllegalArgumentException(
                "Unexpected cpFailureAction: " + application.getCpFailureAction());
          }
          switch (failoverStrategy) {
            case ALERT:
              alertService.alert(application, CheckPointStatus.FAILED);
              break;
            case RESTART:
              try {
                applicationService.restart(application);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              break;
            default:
              // do nothing
              break;
          }
        } else {
          counter.increment();
        }
      }
    }
  }

  private boolean checkSaveForCheckpoint(
      @Nonnull CheckPoints.CheckPoint checkPoint, Long latestId) {
    return latestId == null || !latestId.equals(checkPoint.getId());
  }

  private boolean checkSaveForSavepoint(
      CheckPointKey checkPointKey, @Nonnull CheckPoints.CheckPoint checkPoint) {
    return savepointedCache.getIfPresent(checkPointKey.getSavePointId()) == null
        // If the savepoint triggered before SAVEPOINT_CACHE_HOUR span, we'll see it as out-of-time
        // savepoint and ignore it.
        && checkPoint.getTriggerTimestamp()
            >= System.currentTimeMillis() - TimeUnit.HOURS.toMillis(SAVEPOINT_CACHE_HOUR);
  }

  @Nullable
  private Long getLatestCheckpointId(Long appId, String checkpointId) {
    return checkPointCache.get(
        checkpointId,
        key -> {
          Savepoint savepoint = savepointService.getLatest(appId);
          return Optional.ofNullable(savepoint).map(Savepoint::getChkId).orElse(null);
        });
  }

  private boolean shouldProcessFailedTrigger(
      CheckPoints.CheckPoint checkPoint, boolean cpFailedTrigger, CheckPointStatus status) {
    return CheckPointStatus.FAILED.equals(status)
        && !checkPoint.getIsSavepoint()
        && cpFailedTrigger;
  }

  private void saveSavepoint(CheckPoints.CheckPoint checkPoint, Long appId) {
    Savepoint savepoint = new Savepoint();
    savepoint.setAppId(appId);
    savepoint.setChkId(checkPoint.getId());
    savepoint.setLatest(true);
    savepoint.setType(checkPoint.getCheckPointType().get());
    savepoint.setPath(checkPoint.getExternalPath());
    savepoint.setTriggerTime(new Date(checkPoint.getTriggerTimestamp()));
    savepoint.setCreateTime(new Date());
    savepointService.save(savepoint);
  }

  public static class Counter {
    private final Long timestamp;
    private final AtomicInteger count;

    public Counter(Long timestamp) {
      this.timestamp = timestamp;
      this.count = new AtomicInteger(1);
    }

    public void increment() {
      this.count.incrementAndGet();
    }

    public Integer getCount() {
      return count.get();
    }

    public long getDuration(Long currentTimestamp) {
      return (currentTimestamp - this.timestamp) / 1000 / 60;
    }
  }

  /** Util class for checkpoint key. */
  @Getter
  @Setter
  public static class CheckPointKey {
    private Long appId;
    private String jobId;
    private Long checkId;

    public CheckPointKey(Long appId, String jobId, Long checkId) {
      this.appId = appId;
      this.jobId = jobId;
      this.checkId = checkId;
    }

    /** Get savepoint cache id, see {@link #savepointedCache}. */
    public String getSavePointId() {
      return String.format("%s_%s_%s", appId, jobId, checkId);
    }

    /** Get checkpoint cache id. */
    public String getCheckPointId() {
      return String.format("%s_%s", appId, jobId);
    }
  }
}
