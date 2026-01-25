import React, { useState, useEffect } from 'react';
import { X, Plus, User, Users, Building2, Check, ChevronDown } from 'lucide-react';
import { CasAdminApi } from '../lib/api';

/**
 * ViewerConfigPanel - Enterprise-level, user-friendly UI for configuring viewers
 * Features:
 * - Dropdown-based selection (no typing required)
 * - Multi-select support for roles, departments, and users
 * - Visual chips/tags for selected items
 * - Non-technical language
 */
const ViewerConfigPanel = ({ viewers = [], onChange, title = "Who can view?" }) => {
    // Dropdown data from backend
    const [roles, setRoles] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);

    // Selected items (derived from viewers prop)
    const selectedRoles = viewers.filter(v => v.type === 'ROLE').map(v => v.role);
    const selectedDepartments = viewers.filter(v => v.type === 'DEPARTMENT').map(v => v.departmentId);
    const selectedUsers = viewers.filter(v => v.type === 'USER').map(v => v.userId);

    // Dropdown open states
    const [roleDropdownOpen, setRoleDropdownOpen] = useState(false);
    const [deptDropdownOpen, setDeptDropdownOpen] = useState(false);
    const [userDropdownOpen, setUserDropdownOpen] = useState(false);

    useEffect(() => {
        loadDropdownData();
    }, []);

    const loadDropdownData = async () => {
        try {
            setLoading(true);
            const [rolesData, deptsData, usersData] = await Promise.all([
                CasAdminApi.getRoles().catch(() => []),
                CasAdminApi.getDepartments().catch(() => []),
                CasAdminApi.getUsers().catch(() => [])
            ]);

            // Fallback data if API returns empty
            setRoles(rolesData.length > 0 ? rolesData : [
                { id: '1', code: 'MMS_VIEWER', name: 'Memo Viewer', label: 'Memo Viewer' },
                { id: '2', code: 'MMS_AUDITOR', name: 'Auditor', label: 'Auditor' },
                { id: '3', code: 'MMS_MANAGER', name: 'Manager', label: 'Manager' },
                { id: '4', code: 'MMS_EXECUTIVE', name: 'Executive', label: 'Executive' },
                { id: '5', code: 'MMS_COMPLIANCE', name: 'Compliance Officer', label: 'Compliance Officer' }
            ]);

            setDepartments(deptsData.length > 0 ? deptsData : [
                { id: '1', code: 'FINANCE', name: 'Finance Department' },
                { id: '2', code: 'HR', name: 'Human Resources' },
                { id: '3', code: 'LEGAL', name: 'Legal Department' },
                { id: '4', code: 'AUDIT', name: 'Internal Audit' },
                { id: '5', code: 'OPERATIONS', name: 'Operations' }
            ]);

            setUsers(usersData.length > 0 ? usersData : [
                { id: 'u1', userId: 'admin-001', username: 'Admin User', email: 'admin@company.com' },
                { id: 'u2', userId: 'manager-001', username: 'John Manager', email: 'john@company.com' },
                { id: 'u3', userId: 'ceo-001', username: 'CEO', email: 'ceo@company.com' }
            ]);
        } catch (error) {
            console.error('Failed to load dropdown data:', error);
        } finally {
            setLoading(false);
        }
    };

    // Toggle role selection
    const toggleRole = (roleCode) => {
        const isSelected = selectedRoles.includes(roleCode);
        let newViewers;

        if (isSelected) {
            // Remove role
            newViewers = viewers.filter(v => !(v.type === 'ROLE' && v.role === roleCode));
        } else {
            // Add role
            newViewers = [...viewers, { type: 'ROLE', role: roleCode }];
        }
        onChange(newViewers);
    };

    // Toggle department selection
    const toggleDepartment = (deptCode) => {
        const isSelected = selectedDepartments.includes(deptCode);
        let newViewers;

        if (isSelected) {
            newViewers = viewers.filter(v => !(v.type === 'DEPARTMENT' && v.departmentId === deptCode));
        } else {
            newViewers = [...viewers, { type: 'DEPARTMENT', departmentId: deptCode }];
        }
        onChange(newViewers);
    };

    // Toggle user selection
    const toggleUser = (userId) => {
        const isSelected = selectedUsers.includes(userId);
        let newViewers;

        if (isSelected) {
            newViewers = viewers.filter(v => !(v.type === 'USER' && v.userId === userId));
        } else {
            newViewers = [...viewers, { type: 'USER', userId: userId }];
        }
        onChange(newViewers);
    };

    // Remove a viewer chip
    const removeViewer = (viewer) => {
        onChange(viewers.filter(v => {
            if (v.type === 'ROLE') return !(v.type === viewer.type && v.role === viewer.role);
            if (v.type === 'DEPARTMENT') return !(v.type === viewer.type && v.departmentId === viewer.departmentId);
            if (v.type === 'USER') return !(v.type === viewer.type && v.userId === viewer.userId);
            return true;
        }));
    };

    // Get display name for a viewer
    const getViewerDisplay = (viewer) => {
        if (viewer.type === 'ROLE') {
            const role = roles.find(r => r.code === viewer.role);
            return role ? role.label || role.name : viewer.role;
        }
        if (viewer.type === 'DEPARTMENT') {
            const dept = departments.find(d => d.code === viewer.departmentId);
            return dept ? dept.name : viewer.departmentId;
        }
        if (viewer.type === 'USER') {
            const user = users.find(u => u.userId === viewer.userId);
            return user ? user.username || user.email : viewer.userId;
        }
        return 'Unknown';
    };

    // Get icon for viewer type
    const getViewerIcon = (type) => {
        switch (type) {
            case 'USER': return <User className="w-3 h-3" />;
            case 'ROLE': return <Users className="w-3 h-3" />;
            case 'DEPARTMENT': return <Building2 className="w-3 h-3" />;
            default: return <Users className="w-3 h-3" />;
        }
    };

    // Get color for viewer type chip
    const getViewerColor = (type) => {
        switch (type) {
            case 'USER': return 'bg-purple-100 text-purple-800 border-purple-200';
            case 'ROLE': return 'bg-blue-100 text-blue-800 border-blue-200';
            case 'DEPARTMENT': return 'bg-green-100 text-green-800 border-green-200';
            default: return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };

    if (loading) {
        return (
            <div className="space-y-3">
                <div className="animate-pulse h-8 bg-gray-200 rounded"></div>
                <div className="animate-pulse h-8 bg-gray-200 rounded"></div>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {title && <h3 className="text-sm font-medium text-gray-700">{title}</h3>}

            {/* Selected Viewers - Visual Chips */}
            {viewers.length > 0 && (
                <div className="flex flex-wrap gap-2 p-3 bg-gray-50 rounded-lg border">
                    {viewers.map((viewer, index) => (
                        <span
                            key={index}
                            className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium border ${getViewerColor(viewer.type)}`}
                        >
                            {getViewerIcon(viewer.type)}
                            <span>{getViewerDisplay(viewer)}</span>
                            <button
                                onClick={() => removeViewer(viewer)}
                                className="ml-1 hover:bg-white/50 rounded-full p-0.5"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        </span>
                    ))}
                </div>
            )}

            {/* Selection Dropdowns */}
            <div className="space-y-3">
                {/* Roles Dropdown */}
                <div className="relative">
                    <button
                        type="button"
                        onClick={() => {
                            setRoleDropdownOpen(!roleDropdownOpen);
                            setDeptDropdownOpen(false);
                            setUserDropdownOpen(false);
                        }}
                        className="w-full flex items-center justify-between p-3 bg-white border rounded-lg hover:bg-gray-50 transition-colors"
                    >
                        <div className="flex items-center gap-2">
                            <Users className="w-4 h-4 text-blue-600" />
                            <span className="text-sm font-medium">By Role</span>
                            {selectedRoles.length > 0 && (
                                <span className="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded-full">
                                    {selectedRoles.length} selected
                                </span>
                            )}
                        </div>
                        <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${roleDropdownOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {roleDropdownOpen && (
                        <div className="absolute z-10 w-full mt-1 bg-white border rounded-lg shadow-lg max-h-48 overflow-y-auto">
                            {roles.map(role => (
                                <button
                                    key={role.code}
                                    type="button"
                                    onClick={() => toggleRole(role.code)}
                                    className="w-full flex items-center justify-between p-3 hover:bg-blue-50 transition-colors text-left"
                                >
                                    <span className="text-sm">{role.label || role.name}</span>
                                    {selectedRoles.includes(role.code) && (
                                        <Check className="w-4 h-4 text-blue-600" />
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* Departments Dropdown */}
                <div className="relative">
                    <button
                        type="button"
                        onClick={() => {
                            setDeptDropdownOpen(!deptDropdownOpen);
                            setRoleDropdownOpen(false);
                            setUserDropdownOpen(false);
                        }}
                        className="w-full flex items-center justify-between p-3 bg-white border rounded-lg hover:bg-gray-50 transition-colors"
                    >
                        <div className="flex items-center gap-2">
                            <Building2 className="w-4 h-4 text-green-600" />
                            <span className="text-sm font-medium">By Department</span>
                            {selectedDepartments.length > 0 && (
                                <span className="bg-green-100 text-green-700 text-xs px-2 py-0.5 rounded-full">
                                    {selectedDepartments.length} selected
                                </span>
                            )}
                        </div>
                        <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${deptDropdownOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {deptDropdownOpen && (
                        <div className="absolute z-10 w-full mt-1 bg-white border rounded-lg shadow-lg max-h-48 overflow-y-auto">
                            {departments.map(dept => (
                                <button
                                    key={dept.code}
                                    type="button"
                                    onClick={() => toggleDepartment(dept.code)}
                                    className="w-full flex items-center justify-between p-3 hover:bg-green-50 transition-colors text-left"
                                >
                                    <span className="text-sm">{dept.name}</span>
                                    {selectedDepartments.includes(dept.code) && (
                                        <Check className="w-4 h-4 text-green-600" />
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* Specific Users Dropdown */}
                <div className="relative">
                    <button
                        type="button"
                        onClick={() => {
                            setUserDropdownOpen(!userDropdownOpen);
                            setRoleDropdownOpen(false);
                            setDeptDropdownOpen(false);
                        }}
                        className="w-full flex items-center justify-between p-3 bg-white border rounded-lg hover:bg-gray-50 transition-colors"
                    >
                        <div className="flex items-center gap-2">
                            <User className="w-4 h-4 text-purple-600" />
                            <span className="text-sm font-medium">Specific People</span>
                            {selectedUsers.length > 0 && (
                                <span className="bg-purple-100 text-purple-700 text-xs px-2 py-0.5 rounded-full">
                                    {selectedUsers.length} selected
                                </span>
                            )}
                        </div>
                        <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${userDropdownOpen ? 'rotate-180' : ''}`} />
                    </button>

                    {userDropdownOpen && (
                        <div className="absolute z-10 w-full mt-1 bg-white border rounded-lg shadow-lg max-h-48 overflow-y-auto">
                            {users.map(user => (
                                <button
                                    key={user.userId}
                                    type="button"
                                    onClick={() => toggleUser(user.userId)}
                                    className="w-full flex items-center justify-between p-3 hover:bg-purple-50 transition-colors text-left"
                                >
                                    <div className="flex flex-col">
                                        <span className="text-sm font-medium">{user.username}</span>
                                        <span className="text-xs text-gray-500">{user.email}</span>
                                    </div>
                                    {selectedUsers.includes(user.userId) && (
                                        <Check className="w-4 h-4 text-purple-600" />
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Empty State */}
            {viewers.length === 0 && (
                <div className="text-center py-3 text-xs text-gray-500">
                    Select from the options above to grant view access
                </div>
            )}
        </div>
    );
};

export default ViewerConfigPanel;
