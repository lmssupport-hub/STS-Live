package com.example.nexus.service;

import com.example.nexus.dto.PackageDto;
import com.example.nexus.entity.PackageEntity;
import com.example.nexus.entity.User;
import com.example.nexus.exception.ConflictException;
import com.example.nexus.exception.ResourceNotFoundException;
import com.example.nexus.repository.PackageRepository;
import com.example.nexus.repository.UserRepository;
import com.example.nexus.util.PermissionJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PackageService {

    private final PackageRepository packageRepository;
    private final PermissionJsonMapper permissionJsonMapper;
    private final UserRepository userRepository;

    @Autowired
    public PackageService(PackageRepository packageRepository,
                           PermissionJsonMapper permissionJsonMapper,
                           UserRepository userRepository) {
        this.packageRepository = packageRepository;
        this.permissionJsonMapper = permissionJsonMapper;
        this.userRepository = userRepository;
    }

    public List<PackageDto.Response> getAllPackages() {
        return packageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PackageDto.Response getPackageById(Long id) {
        PackageEntity entity = findEntityOrThrow(id);
        return toResponse(entity);
    }

    public PackageDto.Response createPackage(PackageDto.Request request) {
        if (packageRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("A package named '" + request.getName() + "' already exists");
        }

        PackageEntity entity = new PackageEntity();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPermissionsJson(permissionJsonMapper.toJson(request.getPermissions()));

        PackageEntity saved = packageRepository.save(entity);
        return toResponse(saved);
    }

    public PackageDto.Response updatePackage(Long id, PackageDto.Request request) {
        PackageEntity entity = findEntityOrThrow(id);

        if (packageRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new ConflictException("A package named '" + request.getName() + "' already exists");
        }

        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPermissionsJson(permissionJsonMapper.toJson(request.getPermissions()));

        PackageEntity saved = packageRepository.save(entity);
        return toResponse(saved);
    }

    public void deletePackage(Long id) {
        if (!packageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Package not found with id: " + id);
        }
        packageRepository.deleteById(id);
    }

    public void assignPackageToUser(Long packageId, Long userId) {
        PackageEntity pkg = findEntityOrThrow(packageId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setAssignedPackageId(pkg.getId());
        userRepository.save(user);
    }

    public void unassignPackageFromUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setAssignedPackageId(null);
        userRepository.save(user);
    }

    private PackageEntity findEntityOrThrow(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));
    }

    private PackageDto.Response toResponse(PackageEntity entity) {
        PackageDto.Response response = new PackageDto.Response();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setPermissions(permissionJsonMapper.fromJson(entity.getPermissionsJson()));
        return response;
    }
}