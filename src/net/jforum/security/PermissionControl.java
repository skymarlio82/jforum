
package net.jforum.security;

import java.io.Serializable;

import net.jforum.dao.GroupSecurityDAO;

@SuppressWarnings("serial")
public class PermissionControl implements Serializable {

	private RoleCollection roles = null;

	private transient GroupSecurityDAO smodel = null;

	public void setRoles(RoleCollection roles) {
		this.roles = roles;
	}

	public void setSecurityModel(GroupSecurityDAO smodel) {
		this.smodel = smodel;
	}

	public void addRole(int id, Role role) {
		smodel.addRole(id, role);
	}

	public void addRole(int id, Role role, RoleValueCollection roleValues) {
		smodel.addRole(id, role, roleValues);
	}

	public void addRoleValue(int id, Role role, RoleValueCollection roleValues) {
		smodel.addRoleValue(id, role, roleValues);
	}

	public void deleteAllRoles(int id) {
		smodel.deleteAllRoles(id);
	}

	public Role getRole(String roleName) {
		return roles.get(roleName);
	}

	public boolean canAccess(String roleName) {
		System.out.println("--> [PermissionControl.canAccess] ......");
		System.out.println("DEBUG: the roleName of '" + roleName + "' is included in roles or not : " + roles.containsKey(roleName));
		return roles.containsKey(roleName);
	}

	public boolean canAccess(String roleName, String roleValue) {
		System.out.println("--> [PermissionControl.canAccess] ......");
		Role role = roles.get(roleName);
		if (role == null) {
			System.out.println("DEBUG: the roleName of '" + roleName + "' in roles is NULL");
			return false;
		}
		return role.getValues().contains(new RoleValue(roleValue));
	}
}
