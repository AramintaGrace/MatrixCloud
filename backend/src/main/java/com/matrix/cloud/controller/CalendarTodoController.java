package com.matrix.cloud.controller;

import com.matrix.cloud.dto.CalendarTodoRequest;
import com.matrix.cloud.entity.CalendarTodo;
import com.matrix.cloud.service.CalendarTodoService;
import com.matrix.cloud.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarTodoController {

    @Autowired
    private CalendarTodoService todoService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @GetMapping("/todos")
    public ResponseEntity<List<CalendarTodo>> getTodosByDate(
            @RequestParam String date,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(todoService.getTodosByDate(userId, LocalDate.parse(date)));
    }

    @GetMapping("/month")
    public ResponseEntity<Map<String, List<CalendarTodo>>> getTodosByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(todoService.getTodosByMonth(userId, year, month));
    }

    @PostMapping("/todos")
    public ResponseEntity<CalendarTodo> addTodo(
            @RequestBody CalendarTodoRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(todoService.addTodo(userId, request));
    }

    @PutMapping("/todos/{id}")
    public ResponseEntity<CalendarTodo> updateTodo(
            @PathVariable Long id,
            @RequestBody CalendarTodoRequest request,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(todoService.updateTodo(id, userId, request));
    }

    @PutMapping("/todos/{id}/toggle")
    public ResponseEntity<CalendarTodo> toggleDone(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        todoService.toggleDone(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/todos/{id}")
    public ResponseEntity<Map<String, Object>> deleteTodo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        todoService.deleteTodo(id, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
