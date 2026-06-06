package com.matrix.cloud.repository;

import com.matrix.cloud.entity.CalendarTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarTodoRepository extends JpaRepository<CalendarTodo, Long> {
    List<CalendarTodo> findByUserIdAndTodoDate(Long userId, LocalDate todoDate);
    List<CalendarTodo> findByUserIdAndTodoDateBetween(Long userId, LocalDate start, LocalDate end);
    void deleteByUserIdAndTodoDate(Long userId, LocalDate todoDate);
}
