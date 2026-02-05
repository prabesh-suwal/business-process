import React from 'react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { X, MapPin, Building2, Users, UserCircle, ChevronDown, ChevronUp } from 'lucide-react';
import SearchableMultiSelect from './ui/SearchableMultiSelect';

/**
 * AssignmentRuleCard - A single assignment rule with multiple criteria.
 * All criteria within a rule are combined with AND logic.
 * Clean, minimal styling to match reference design.
 */
const AssignmentRuleCard = ({
    rule,
    ruleIndex,
    onUpdate,
    onRemove,
    geoOptions = {},      // { regions, districts, states }
    orgOptions = {},      // { branches, departments, groups, roles, users }
    isExpanded = true,
    onToggleExpand
}) => {
    const { criteria = {} } = rule;

    // Helper to update criteria
    const updateCriteria = (field, value) => {
        onUpdate({
            ...rule,
            criteria: {
                ...criteria,
                [field]: value
            }
        });
    };

    // Count how many criteria types are configured
    const configuredCount = [
        criteria.regionIds?.length,
        criteria.districtIds?.length,
        criteria.stateIds?.length,
        criteria.branchIds?.length,
        criteria.departmentIds?.length,
        criteria.groupIds?.length,
        criteria.roleIds?.length,
        criteria.userIds?.length
    ].filter(Boolean).length;

    return (
        <Card className="border border-slate-200 hover:border-blue-300 transition-colors bg-white">
            {/* Rule Header - Clean and compact */}
            <div
                className="flex items-center justify-between px-3 py-2 cursor-pointer bg-slate-50 border-b border-slate-100"
                onClick={onToggleExpand}
            >
                <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-semibold">
                        {ruleIndex + 1}
                    </div>
                    <input
                        type="text"
                        value={rule.name || ''}
                        onChange={(e) => onUpdate({ ...rule, name: e.target.value })}
                        placeholder={`Rule ${ruleIndex + 1}`}
                        className="text-sm font-medium text-slate-900 bg-transparent border-none focus:outline-none focus:ring-1 focus:ring-blue-500 rounded px-1"
                        onClick={(e) => e.stopPropagation()}
                    />
                    {configuredCount > 0 && (
                        <span className="text-[10px] text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                            {configuredCount} criteria
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-1">
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={(e) => { e.stopPropagation(); onRemove(); }}
                        className="text-red-500 hover:text-red-700 hover:bg-red-50 h-7 w-7 p-0"
                    >
                        <X className="w-3.5 h-3.5" />
                    </Button>
                    {isExpanded ? (
                        <ChevronUp className="w-4 h-4 text-slate-400" />
                    ) : (
                        <ChevronDown className="w-4 h-4 text-slate-400" />
                    )}
                </div>
            </div>

            {/* Rule Content - Collapsible with clean sections */}
            {isExpanded && (
                <CardContent className="p-3 space-y-4">
                    {/* Geography Section */}
                    <div className="space-y-2">
                        <div className="flex items-center gap-2 text-slate-600">
                            <MapPin className="w-3.5 h-3.5" />
                            <span className="text-xs font-semibold uppercase tracking-wider">Geography</span>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
                            <SearchableMultiSelect
                                options={(geoOptions.regions || []).map(r => ({ value: r.id, label: r.name }))}
                                selected={criteria.regionIds || []}
                                onChange={(selected) => updateCriteria('regionIds', selected)}
                                placeholder="Search regions..."
                                label="Regions"
                            />
                            <SearchableMultiSelect
                                options={(geoOptions.districts || [])
                                    .filter(d => !criteria.regionIds?.length || criteria.regionIds.includes(d.regionId))
                                    .map(d => ({ value: d.id, label: d.name }))}
                                selected={criteria.districtIds || []}
                                onChange={(selected) => updateCriteria('districtIds', selected)}
                                placeholder="Search districts..."
                                label="Districts"
                            />
                            <SearchableMultiSelect
                                options={(geoOptions.states || [])
                                    .filter(s => !criteria.districtIds?.length || criteria.districtIds.includes(s.districtId))
                                    .map(s => ({ value: s.id, label: s.name }))}
                                selected={criteria.stateIds || []}
                                onChange={(selected) => updateCriteria('stateIds', selected)}
                                placeholder="Search states..."
                                label="States"
                            />
                        </div>
                    </div>

                    {/* Organization Section */}
                    <div className="space-y-2">
                        <div className="flex items-center gap-2 text-slate-600">
                            <Building2 className="w-3.5 h-3.5" />
                            <span className="text-xs font-semibold uppercase tracking-wider">Organization</span>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                            <SearchableMultiSelect
                                options={(orgOptions.branches || []).map(b => ({ value: b.id, label: b.name }))}
                                selected={criteria.branchIds || []}
                                onChange={(selected) => updateCriteria('branchIds', selected)}
                                placeholder="Search branches..."
                                label="Branches"
                            />
                            <SearchableMultiSelect
                                options={(orgOptions.departments || []).map(d => ({ value: d.id, label: d.name }))}
                                selected={criteria.departmentIds || []}
                                onChange={(selected) => updateCriteria('departmentIds', selected)}
                                placeholder="Search departments..."
                                label="Departments"
                            />
                        </div>
                    </div>

                    {/* Groups & Roles Section */}
                    <div className="space-y-2">
                        <div className="flex items-center gap-2 text-slate-600">
                            <Users className="w-3.5 h-3.5" />
                            <span className="text-xs font-semibold uppercase tracking-wider">Roles</span>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                            <SearchableMultiSelect
                                options={(orgOptions.groups || []).map(g => ({ value: g.id, label: g.name }))}
                                selected={criteria.groupIds || []}
                                onChange={(selected) => updateCriteria('groupIds', selected)}
                                placeholder="Search groups..."
                                label="Groups"
                            />
                            <SearchableMultiSelect
                                options={(orgOptions.roles || []).map(r => ({ value: r.id, label: r.name || r.code }))}
                                selected={criteria.roleIds || []}
                                onChange={(selected) => updateCriteria('roleIds', selected)}
                                placeholder="Search roles..."
                                label="Roles"
                            />
                        </div>
                    </div>

                    {/* Specific Users Section */}
                    <div className="space-y-2">
                        <div className="flex items-center gap-2 text-slate-600">
                            <UserCircle className="w-3.5 h-3.5" />
                            <span className="text-xs font-semibold uppercase tracking-wider">Specific Users</span>
                        </div>
                        <SearchableMultiSelect
                            options={(orgOptions.users || []).map(u => ({
                                value: u.id,
                                label: u.fullName || u.username
                            }))}
                            selected={criteria.userIds || []}
                            onChange={(selected) => updateCriteria('userIds', selected)}
                            placeholder="Search users..."
                            label="Users"
                        />
                    </div>

                    {/* Info - Minimal */}
                    <p className="text-[10px] text-slate-400 italic pt-1">
                        Users must match all selected criteria (AND logic)
                    </p>
                </CardContent>
            )}
        </Card>
    );
};

export default AssignmentRuleCard;
