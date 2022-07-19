package com.streamxhub.streamx.console.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.system.entity.Team;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.service.TeamService;
import com.streamxhub.streamx.console.system.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * @author daixinyu
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/team")
public class TeamController {

    @Autowired
    private TeamService teamService;


    @PostMapping("list")
    @RequiresPermissions("team:view")
    public RestResponse teamList(RestRequest restRequest, Team team) {
        IPage<Team> groupList = teamService.findTeams(team, restRequest);
        return RestResponse.create().data(groupList);
    }

    @PostMapping("/listByUser")
    public RestResponse listByUser(RestRequest restRequest, Team team) {
        IPage<Team> teamList = teamService.findTeamsByNowUser(team, restRequest);
        return RestResponse.create().data(teamList);
    }

    @PostMapping("post")
    @RequiresPermissions("team:add")
    public RestResponse addTeam(@Valid Team team) throws Exception {
        this.teamService.createTeam(team);
        return RestResponse.create();
    }

    @DeleteMapping("delete")
    @RequiresPermissions("team:delete")
    public RestResponse deleteTeam(Long teamId) {
        String result = this.teamService.deleteTeamBeforeCheck(teamId);
        return RestResponse.create().message(result);
    }

    @PostMapping("check/name")
    public RestResponse checkTeamName(@NotBlank(message = "{required}") String teamName) {
        boolean result = this.teamService.findByName(teamName) == null;
        return RestResponse.create().data(result);
    }


    @PostMapping("check/code")
    public RestResponse checkTeamCode(@NotBlank(message = "{required}") String teamCode) {
        boolean result = this.teamService.findByCode(teamCode) == null;
        return RestResponse.create().data(result);
    }
}
