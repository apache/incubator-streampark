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

package org.apache.streampark.console.system.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.Team;
import org.apache.streampark.console.system.entity.User;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MemberService extends IService<Member> {

  void deleteByUserId(Long userId);

  void deleteByTeamId(Long teamId);

  IPage<Member> page(Member member, RestRequest request);

  List<User> findCandidateUsers(Long teamId);

  List<Team> findUserTeams(Long userId);

  Member findByUserId(Long teamId, Long userId);

  List<Long> findUserIdsByRoleId(Long roleId);

  void createMember(Member member);

  void deleteMember(Member member);

  void updateMember(Member member);
}
