package com.example.nexus.controller;

import com.example.nexus.dto.PackageDto;
import com.example.nexus.service.PackageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final PackageService packageService;

    @Autowired
    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ResponseEntity<List<PackageDto.Response>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDto.Response> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }

    @PostMapping
    public ResponseEntity<PackageDto.Response> createPackage(@Valid @RequestBody PackageDto.Request request) {
        PackageDto.Response response = packageService.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PackageDto.Response> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageDto.Request request) {
        return ResponseEntity.ok(packageService.updatePackage(id, request));
    }

    @PostMapping("/{packageId}/assign/{userId}")
    public ResponseEntity<Void> assignPackage(
            @PathVariable Long packageId,
            @PathVariable Long userId) {
        packageService.assignPackageToUser(packageId, userId);
        return ResponseEntity.ok().build();
    }

 
    @DeleteMapping("/assign/{userId}")
    public ResponseEntity<Void> unassignPackage(@PathVariable Long userId) {
        packageService.unassignPackageFromUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}