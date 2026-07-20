package com.example.nexus.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.nexus.entity.Project;
import com.example.nexus.service.ProjectService;
import com.example.nexus.util.JwtUtil;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin("*")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractEmail(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractEmail(token);
    }

    @PostMapping
    public Project createProject(@RequestBody Project project,
                                  @RequestHeader("Authorization") String authHeader) {
        return projectService.createProject(project, extractEmail(authHeader));
    }

    @GetMapping
    public List<Project> getAllProjects(@RequestHeader("Authorization") String authHeader) {
        return projectService.getAllProjects(extractEmail(authHeader));
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id,
                                   @RequestHeader("Authorization") String authHeader) {
        return projectService.getProjectById(id, extractEmail(authHeader));
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project project,
                                  @RequestHeader("Authorization") String authHeader) {
        return projectService.updateProject(id, project, extractEmail(authHeader));
    }

    @DeleteMapping("/{id}")
    public String deleteProject(@PathVariable Long id,
                                 @RequestHeader("Authorization") String authHeader) {
        projectService.deleteProject(id, extractEmail(authHeader));
        return "Project deleted successfully";
    }
}