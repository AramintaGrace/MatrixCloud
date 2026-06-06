package com.matrix.cloud.service;

import com.matrix.cloud.dto.CalendarTodoRequest;
import com.matrix.cloud.entity.CalendarTodo;
import com.matrix.cloud.repository.CalendarTodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CalendarTodoService {

    @Autowired
    private CalendarTodoRepository todoRepository;

    public List<CalendarTodo> getTodosByDate(Long userId, LocalDate date) {
        return todoRepository.findByUserIdAndTodoDate(userId, date);
    }

    public Map<String, List<CalendarTodo>> getTodosByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<CalendarTodo> todos = todoRepository.findByUserIdAndTodoDateBetween(userId, start, end);
        Map<String, List<CalendarTodo>> grouped = new java.util.LinkedHashMap<>();
        for (CalendarTodo todo : todos) {
            grouped.computeIfAbsent(todo.getTodoDate().toString(), k -> new java.util.ArrayList<>()).add(todo);
        }
        return grouped;
    }

    @Transactional
    public CalendarTodo addTodo(Long userId, CalendarTodoRequest request) {
        CalendarTodo todo = new CalendarTodo();
        todo.setUserId(userId);
        todo.setTodoDate(LocalDate.parse(request.getDate()));
        todo.setContent(request.getContent());
        todo.setCreatedAt(LocalDateTime.now());
        return todoRepository.save(todo);
    }

    @Transactional
    public CalendarTodo updateTodo(Long todoId, Long userId, CalendarTodoRequest request) {
        CalendarTodo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("待办事项不存在"));
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改");
        }
        todo.setContent(request.getContent());
        todo.setUpdatedAt(LocalDateTime.now());
        return todoRepository.save(todo);
    }

    @Transactional
    public void toggleDone(Long todoId, Long userId) {
        CalendarTodo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("待办事项不存在"));
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改");
        }
        todo.setDone(!Boolean.TRUE.equals(todo.getDone()));
        todo.setUpdatedAt(LocalDateTime.now());
        todoRepository.save(todo);
    }

    @Transactional
    public void deleteTodo(Long todoId, Long userId) {
        CalendarTodo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new RuntimeException("待办事项不存在"));
        if (!todo.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除");
        }
        todoRepository.delete(todo);
    }

    @Transactional
    public void clearTodosByDate(Long userId, LocalDate date) {
        todoRepository.deleteByUserIdAndTodoDate(userId, date);
    }
}
