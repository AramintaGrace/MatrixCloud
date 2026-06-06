package com.matrix.cloud.service;

import com.matrix.cloud.entity.ChatSession;
import com.matrix.cloud.entity.Team;
import com.matrix.cloud.entity.TeamMember;
import com.matrix.cloud.entity.User;
import com.matrix.cloud.repository.ChatSessionRepository;
import com.matrix.cloud.repository.TeamMemberRepository;
import com.matrix.cloud.repository.TeamRepository;
import com.matrix.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Team createTeam(String name, Long creatorId) {
        if (teamRepository.existsByName(name)) {
            throw new RuntimeException("团队名称已存在");
        }

        Team team = new Team();
        team.setName(name);
        team.setCreatorId(creatorId);
        team = teamRepository.save(team);

        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(creatorId);
        member.setRole(TeamMember.Role.ADMIN);
        teamMemberRepository.save(member);

        ChatSession chatSession = new ChatSession();
        chatSession.setType(ChatSession.Type.TEAM);
        chatSession.setTeamId(team.getId());
        chatSessionRepository.save(chatSession);

        return team;
    }

    @Transactional
    public void addMember(Long teamId, Long userId) {
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new RuntimeException("用户已在团队中");
        }

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole(TeamMember.Role.MEMBER);
        teamMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long teamId, Long userId, Long operatorId) {
        TeamMember operator = teamMemberRepository.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new RuntimeException("您不是团队成员"));

        if (operator.getRole() != TeamMember.Role.ADMIN) {
            throw new RuntimeException("只有管理员可以移除成员");
        }

        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    public List<TeamMember> getTeamMembers(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    public List<Map<String, Object>> getTeamMembersWithDetails(Long teamId) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        return members.stream().map(member -> {
            User user = userRepository.findById(member.getUserId())
                    .orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("id", member.getId());
            map.put("userId", member.getUserId());
            map.put("nickname", user != null ? user.getNickname() : "未知用户");
            map.put("email", user != null ? user.getEmail() : "");
            map.put("avatar", user != null ? user.getAvatar() : null);
            map.put("role", member.getRole().name());
            map.put("joinedAt", member.getJoinedAt().toString());
            return map;
        }).toList();
    }

    public List<TeamMember> getUserTeams(Long userId) {
        return teamMemberRepository.findByUserId(userId);
    }

    public List<Map<String, Object>> getUserTeamsWithDetails(Long userId) {
        List<TeamMember> members = teamMemberRepository.findByUserId(userId);
        return members.stream().map(member -> {
            Team team = teamRepository.findById(member.getTeamId())
                    .orElseThrow(() -> new RuntimeException("团队不存在"));
            long memberCount = teamMemberRepository.countByTeamId(team.getId());

            Map<String, Object> map = new HashMap<>();
            map.put("teamId", team.getId());
            map.put("teamName", team.getName());
            map.put("creatorId", team.getCreatorId());
            map.put("memberCount", memberCount);
            map.put("role", member.getRole().name());
            return map;
        }).toList();
    }

    @Transactional
    public void updateMemberRole(Long teamId, Long targetUserId, Long operatorId, String newRole) {
        TeamMember operator = teamMemberRepository.findByTeamIdAndUserId(teamId, operatorId)
                .orElseThrow(() -> new RuntimeException("您不是团队成员"));
        if (operator.getRole() != TeamMember.Role.ADMIN) {
            throw new RuntimeException("只有管理员可以修改成员角色");
        }

        TeamMember target = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不在团队中"));

        Team team = teamRepository.findById(teamId).orElseThrow();
        if (team.getCreatorId().equals(targetUserId)) {
            throw new RuntimeException("不能修改团队创建者的角色");
        }

        target.setRole(TeamMember.Role.valueOf(newRole.toUpperCase()));
        teamMemberRepository.save(target);
    }

    @Transactional
    public void deleteTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("团队不存在"));

        if (!team.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有团队创建者可以解散团队");
        }

        teamMemberRepository.deleteByTeamId(teamId);
        teamRepository.delete(team);
    }
}
