package com.example.nexus.dto;

import java.util.List;

public class EffectivePermissionsDto {

    private String roleType;       
    private boolean fullAccess;    
    private List<PackageDto.Category> permissions;

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public boolean isFullAccess() { return fullAccess; }
    public void setFullAccess(boolean fullAccess) { this.fullAccess = fullAccess; }

    public List<PackageDto.Category> getPermissions() { return permissions; }
    public void setPermissions(List<PackageDto.Category> permissions) { this.permissions = permissions; }
}