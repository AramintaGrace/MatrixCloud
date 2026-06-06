package com.matrix.cloud.controller;

import com.matrix.cloud.dto.ApiResponse;
import com.matrix.cloud.entity.Team;
import com.matrix.cloud.service.TeamService;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ApiResponse<Team> createTeam(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody Map<String, String> request) {
        Long userId = getUserIdFromToken(authHeader);
        String name = request.get("name");
        Team team = teamService.createTeam(name, userId);
        return ApiResponse.success("团队创建成功", team);
    }

    @PostMapping("/{teamId}/members")
    public ApiResponse<Void> addMember(@PathVariable Long teamId,
                                       @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        teamService.addMember(teamId, userId);
        return ApiResponse.success("成员添加成功", null);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long teamId,
                                          @PathVariable Long userId,
                                          @RequestHeader("Authorization") String authHeader) {
        Long operatorId = getUserIdFromToken(authHeader);
        teamService.removeMember(teamId, userId, operatorId);
        return ApiResponse.success("成员移除成功", null);
    }

    @GetMapping("/{teamId}/members")
    public ApiResponse<List<Map<String, Object>>> getTeamMembers(@PathVariable Long teamId) {
        List<Map<String, Object>> members = teamService.getTeamMembersWithDetails(teamId);
        return ApiResponse.success(members);
    }

    @GetMapping("/my-teams")
    public ApiResponse<List<Map<String, Object>>> getMyTeams(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<Map<String, Object>> teams = teamService.getUserTeamsWithDetails(userId);
        return ApiResponse.success(teams);
    }

    @PutMapping("/{teamId}/members/{userId}/role")
    public ApiResponse<Void> updateMemberRole(@PathVariable Long teamId,
                                               @PathVariable Long userId,
                                               @RequestBody Map<String, String> request,
                                               @RequestHeader("Authorization") String authHeader) {
        Long operatorId = getUserIdFromToken(authHeader);
        String role = request.get("role");
        teamService.updateMemberRole(teamId, userId, operatorId, role);
        return ApiResponse.success("角色更新成功", null);
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId,
                                        @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        teamService.deleteTeam(teamId, userId);
        return ApiResponse.success("团队已解散", null);
    }
}
