import React from 'react';
import { X, Eye, Unlock, Users, Clock, AlertTriangle, Settings2 } from 'lucide-react';
import ViewerConfigPanel from './ViewerConfigPanel';

/**
 * GlobalSettingsModal - Popup for memo-wide viewers & user override permissions.
 * Triggered by the ⚙️ settings button in the workflow designer header.
 */
const GlobalSettingsModal = ({
    isOpen,
    onClose,
    memoWideViewers = [],
    onViewersChange,
    overridePermissions = {},
    onOverridePermissionsChange
}) => {
    if (!isOpen) return null;

    const togglePermission = (key) => {
        onOverridePermissionsChange({
            ...overridePermissions,
            [key]: !overridePermissions[key]
        });
    };

    const permissionToggles = [
        { key: 'allowOverrideAssignments', label: 'Modify assignments', icon: Users, description: 'Let memo creators change who handles each step' },
        { key: 'allowOverrideSLA', label: 'Modify time limits', icon: Clock, description: 'Let memo creators adjust deadlines' },
        { key: 'allowOverrideEscalation', label: 'Modify escalation rules', icon: AlertTriangle, description: 'Let memo creators change escalation behavior' },
        { key: 'allowOverrideViewers', label: 'Add/remove viewers', icon: Eye, description: 'Let memo creators control who can see' },
    ];

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 transition-opacity"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                <div
                    className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden border border-slate-200"
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="px-6 py-4 border-b border-slate-100 bg-gradient-to-r from-slate-50 to-white flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="p-2 rounded-xl bg-gradient-to-br from-slate-700 to-slate-900 shadow-lg">
                                <Settings2 className="w-5 h-5 text-white" />
                            </div>
                            <div>
                                <h2 className="text-lg font-semibold text-slate-900">Workflow Settings</h2>
                                <p className="text-xs text-slate-500">Global viewers & user permissions</p>
                            </div>
                        </div>
                        <button
                            onClick={onClose}
                            className="p-2 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Scrollable Content */}
                    <div className="flex-1 overflow-y-auto">
                        {/* User Override Permissions Section */}
                        <div className="p-6 border-b border-slate-100">
                            <div className="flex items-center gap-2 mb-4">
                                <div className="p-1.5 rounded-lg bg-emerald-100">
                                    <Unlock className="w-4 h-4 text-emerald-600" />
                                </div>
                                <div>
                                    <h3 className="text-sm font-semibold text-slate-800">User Override Permissions</h3>
                                    <p className="text-xs text-slate-500">Control what memo creators can customize</p>
                                </div>
                            </div>

                            <div className="space-y-2">
                                {permissionToggles.map(({ key, label, icon: Icon, description }) => (
                                    <div
                                        key={key}
                                        className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-slate-200 hover:bg-slate-50/50 transition-all"
                                    >
                                        <div className="flex items-center gap-3">
                                            <Icon className="w-4 h-4 text-slate-400" />
                                            <div>
                                                <span className="text-sm font-medium text-slate-700">{label}</span>
                                                <p className="text-xs text-slate-400">{description}</p>
                                            </div>
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => togglePermission(key)}
                                            className={`relative w-11 h-6 rounded-full transition-colors duration-200 flex-shrink-0 ${overridePermissions[key]
                                                    ? 'bg-emerald-500'
                                                    : 'bg-slate-200'
                                                }`}
                                        >
                                            <span
                                                className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200 ${overridePermissions[key] ? 'translate-x-5' : ''
                                                    }`}
                                            />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Memo-Wide Viewers Section */}
                        <div className="p-6">
                            <div className="flex items-center gap-2 mb-4">
                                <div className="p-1.5 rounded-lg bg-indigo-100">
                                    <Eye className="w-4 h-4 text-indigo-600" />
                                </div>
                                <div>
                                    <h3 className="text-sm font-semibold text-slate-800">Memo-Wide Viewers</h3>
                                    <p className="text-xs text-slate-500">Read-only access across all workflow steps</p>
                                </div>
                            </div>

                            <ViewerConfigPanel
                                viewers={memoWideViewers}
                                onChange={onViewersChange}
                                title=""
                            />
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="px-6 py-3 border-t border-slate-100 bg-slate-50 flex justify-end">
                        <button
                            onClick={onClose}
                            className="px-4 py-2 text-sm font-medium rounded-lg bg-slate-900 text-white hover:bg-slate-800 transition-colors shadow-sm"
                        >
                            Done
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};

export default GlobalSettingsModal;
