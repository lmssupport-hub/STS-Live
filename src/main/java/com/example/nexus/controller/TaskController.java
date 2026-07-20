package com.example.nexus.controller;
import com.example.nexus.dto.Taskdtos.*;
import com.example.nexus.service.TaskService;
import com.example.nexus.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final JwtUtil jwtUtil;

    public TaskController(TaskService taskService, JwtUtil jwtUtil) {
        this.taskService = taskService;
        this.jwtUtil = jwtUtil;
    }

    private String extractEmail(String authHeader) {
        return jwtUtil.extractEmail(authHeader.replace("Bearer ", ""));
    }

    // GET /api/tasks?projectId=1
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @RequestParam Long projectId,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId, extractEmail(authHeader)));
    }

    // GET /api/tasks/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(taskService.getTaskById(id, extractEmail(authHeader)));
    }

    // POST /api/tasks
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request, extractEmail(authHeader)));
    }

    // PUT /api/tasks/{id}
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(taskService.updateTask(id, request, extractEmail(authHeader)));
    }

    // DELETE /api/tasks/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        taskService.deleteTask(id, extractEmail(authHeader));
        return ResponseEntity.ok("Task deleted successfully");
    }
}