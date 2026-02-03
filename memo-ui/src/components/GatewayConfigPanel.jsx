import { useState, useEffect } from 'react';
import {
    GitBranch,
    CheckCircle2,
    CircleDot,
    Settings2,
    ChevronDown,
    HelpCircle,
    Zap,
    Users,
    UserCheck,
    Info,
    Sparkles
} from 'lucide-react';

/**
 * GatewayConfigPanel - Premium configuration panel for parallel approval workflows
 * 
 * Designed for non-technical admins/business users with:
 * - Clear, jargon-free labels and descriptions
 * - Visual examples of each mode
 * - Helpful tooltips and guidance
 */
export default function GatewayConfigPanel({
    gateway,              // { id, name, type, incomingFlows, outgoingFlows }
    config,               // Current gateway config
    onUpdate,             // (newConfig) => void
    allGateways = [],     // For showing parallel gateway hierarchy
    nestingLevel = 0,     // For nested parallel visualization
    className = ''
}) {
    const [isExpanded, setIsExpanded] = useState(true);
    const [showHelp, setShowHelp] = useState(false);

    // Determine if this is a join or fork gateway
    const isJoinGateway = gateway.incomingFlows?.length > 1 || gateway.incoming?.length > 1;
    const isForkGateway = gateway.outgoingFlows?.length > 1 || gateway.outgoing?.length > 1;
    const branchCount = gateway.incomingFlows?.length || gateway.incoming?.length || 2;

    // Default config
    const currentConfig = config || {
        gatewayId: gateway.id,
        completionMode: 'ALL',  // ALL, ANY, N_OF_M
        minimumRequired: 1,
        description: ''
    };

    // User-friendly completion mode options
    const completionModes = [
        {
            value: 'ALL',
            label: 'Wait for everyone',
            shortLabel: 'All must approve',
            description: 'The workflow continues only after every approver has completed their review.',
            example: `All ${branchCount} people must approve before proceeding.`,
            icon: Users,
            color: 'blue',
            recommended: true
        },
        {
            value: 'ANY',
            label: 'Continue when any one approves',
            shortLabel: 'First approval wins',
            description: 'The workflow continues as soon as any single approver completes their review.',
            example: 'If any one of the approvers says "yes", the workflow moves forward.',
            icon: Zap,
            color: 'amber'
        },
        {
            value: 'N_OF_M',
            label: 'Require a minimum number',
            shortLabel: 'Custom requirement',
            description: 'Specify exactly how many approvals are needed before the workflow continues.',
            example: 'For example: require 2 out of 3 approvers to agree.',
            icon: UserCheck,
            color: 'purple'
        }
    ];

    const handleModeChange = (mode) => {
        onUpdate({
            ...currentConfig,
            completionMode: mode,
            minimumRequired: mode === 'N_OF_M' ? Math.max(1, currentConfig.minimumRequired || 1) : 1
        });
    };

    const handleMinimumChange = (value) => {
        const max = branchCount;
        const min = Math.max(1, Math.min(parseInt(value) || 1, max));
        onUpdate({
            ...currentConfig,
            minimumRequired: min
        });
    };

    // Only show completion config for join gateways (where branches merge)
    if (!isJoinGateway) {
        return (
            <div className={`gateway-config-panel fork-gateway ${className}`} style={{
                marginLeft: `${nestingLevel * 16}px`
            }}>
                <div className="flex items-center gap-3 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border border-blue-100 shadow-sm">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-200">
                        <GitBranch className="w-6 h-6 text-white" />
                    </div>
                    <div>
                        <h4 className="font-semibold text-blue-900">
                            {gateway.name || 'Parallel Start'}
                        </h4>
                        <p className="text-sm text-blue-600">
                            Splits into {gateway.outgoingFlows?.length || gateway.outgoing?.length || 'multiple'} parallel approval paths
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div
            className={`gateway-config-panel join-gateway ${className}`}
            style={{ marginLeft: `${nestingLevel * 16}px` }}
        >
            <div className="bg-white rounded-2xl border border-purple-100 shadow-lg shadow-purple-100/50 overflow-hidden">
                {/* Header */}
                <div
                    className="relative overflow-hidden cursor-pointer"
                    onClick={() => setIsExpanded(!isExpanded)}
                >
                    {/* Background */}
                    <div className="absolute inset-0 bg-gradient-to-r from-purple-500 via-indigo-500 to-purple-600" />

                    {/* Decorative */}
                    <div className="absolute inset-0 opacity-10">
                        <div className="absolute -top-10 -right-10 w-32 h-32 bg-white rounded-full" />
                    </div>

                    <div className="relative flex items-center justify-between p-5">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center border border-white/30">
                                <GitBranch className="w-6 h-6 text-white" />
                            </div>
                            <div>
                                <h4 className="text-lg font-bold text-white">
                                    {gateway.name || 'Approval Collection Point'}
                                </h4>
                                <p className="text-white/80 text-sm">
                                    {branchCount} approval paths merge here
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="px-3 py-1.5 rounded-full bg-white/20 text-white text-sm font-medium">
                                {completionModes.find(m => m.value === currentConfig.completionMode)?.shortLabel || 'All must approve'}
                            </div>
                            <ChevronDown
                                className={`w-5 h-5 text-white/70 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                            />
                        </div>
                    </div>
                </div>

                {/* Content */}
                {isExpanded && (
                    <div className="p-6 space-y-6 animate-in slide-in-from-top-2 duration-300">
                        {/* Mode Selection */}
                        <div>
                            <div className="flex items-center justify-between mb-4">
                                <label className="text-sm font-semibold text-gray-800 flex items-center gap-2">
                                    <Settings2 className="w-4 h-4 text-purple-500" />
                                    How should approvals work?
                                </label>
                                <button
                                    onClick={() => setShowHelp(!showHelp)}
                                    className="text-xs text-gray-500 hover:text-purple-600 flex items-center gap-1"
                                >
                                    <HelpCircle className="w-3.5 h-3.5" />
                                    {showHelp ? 'Hide help' : 'Need help?'}
                                </button>
                            </div>

                            {/* Help panel */}
                            {showHelp && (
                                <div className="mb-4 p-4 bg-blue-50 rounded-xl border border-blue-100 text-sm text-blue-700">
                                    <div className="flex items-start gap-2">
                                        <Info className="w-4 h-4 mt-0.5 flex-shrink-0" />
                                        <div>
                                            <strong>What does this setting do?</strong>
                                            <p className="mt-1">
                                                When a document needs approval from multiple people at the same time,
                                                this setting controls when the workflow can move to the next step.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div className="space-y-3">
                                {completionModes.map((mode) => {
                                    const Icon = mode.icon;
                                    const isSelected = currentConfig.completionMode === mode.value;

                                    const colorClasses = {
                                        blue: {
                                            bg: isSelected ? 'bg-blue-50' : '',
                                            border: isSelected ? 'border-blue-300 ring-2 ring-blue-100' : 'border-gray-200 hover:border-blue-200',
                                            icon: isSelected ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-500',
                                            text: isSelected ? 'text-blue-900' : 'text-gray-700'
                                        },
                                        amber: {
                                            bg: isSelected ? 'bg-amber-50' : '',
                                            border: isSelected ? 'border-amber-300 ring-2 ring-amber-100' : 'border-gray-200 hover:border-amber-200',
                                            icon: isSelected ? 'bg-amber-500 text-white' : 'bg-gray-100 text-gray-500',
                                            text: isSelected ? 'text-amber-900' : 'text-gray-700'
                                        },
                                        purple: {
                                            bg: isSelected ? 'bg-purple-50' : '',
                                            border: isSelected ? 'border-purple-300 ring-2 ring-purple-100' : 'border-gray-200 hover:border-purple-200',
                                            icon: isSelected ? 'bg-purple-500 text-white' : 'bg-gray-100 text-gray-500',
                                            text: isSelected ? 'text-purple-900' : 'text-gray-700'
                                        }
                                    };

                                    const colors = colorClasses[mode.color];

                                    return (
                                        <div
                                            key={mode.value}
                                            onClick={() => handleModeChange(mode.value)}
                                            className={`
                                                relative p-4 rounded-xl border-2 cursor-pointer transition-all duration-200
                                                ${colors.bg} ${colors.border}
                                            `}
                                        >
                                            {mode.recommended && (
                                                <span className="absolute -top-2 right-4 px-2 py-0.5 bg-blue-500 text-white text-[10px] font-bold rounded-full shadow-sm">
                                                    RECOMMENDED
                                                </span>
                                            )}

                                            <div className="flex items-start gap-4">
                                                <div className={`
                                                    w-10 h-10 rounded-xl flex items-center justify-center transition-all
                                                    ${colors.icon}
                                                `}>
                                                    <Icon className="w-5 h-5" />
                                                </div>
                                                <div className="flex-1">
                                                    <div className={`font-semibold ${colors.text}`}>
                                                        {mode.label}
                                                    </div>
                                                    <div className="text-sm text-gray-500 mt-1">
                                                        {mode.description}
                                                    </div>
                                                    <div className="text-xs text-gray-400 mt-2 italic">
                                                        Example: {mode.example}
                                                    </div>
                                                </div>
                                                {isSelected && (
                                                    <CheckCircle2 className={`w-5 h-5 ${colors.text}`} />
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        {/* N of M Configuration */}
                        {currentConfig.completionMode === 'N_OF_M' && (
                            <div className="p-5 bg-purple-50 rounded-xl border border-purple-100 animate-in slide-in-from-top-2">
                                <label className="text-sm font-semibold text-purple-800 mb-3 block flex items-center gap-2">
                                    <UserCheck className="w-4 h-4" />
                                    How many approvals are needed?
                                </label>
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center bg-white rounded-xl border border-purple-200 overflow-hidden">
                                        <button
                                            onClick={() => handleMinimumChange((currentConfig.minimumRequired || 1) - 1)}
                                            className="px-4 py-3 hover:bg-purple-100 text-purple-600 font-bold text-lg"
                                            disabled={(currentConfig.minimumRequired || 1) <= 1}
                                        >
                                            −
                                        </button>
                                        <input
                                            type="number"
                                            min={1}
                                            max={branchCount}
                                            value={currentConfig.minimumRequired || 1}
                                            onChange={(e) => handleMinimumChange(e.target.value)}
                                            className="w-16 py-3 text-center font-bold text-xl text-purple-700 border-x border-purple-200 focus:outline-none"
                                        />
                                        <button
                                            onClick={() => handleMinimumChange((currentConfig.minimumRequired || 1) + 1)}
                                            className="px-4 py-3 hover:bg-purple-100 text-purple-600 font-bold text-lg"
                                            disabled={(currentConfig.minimumRequired || 1) >= branchCount}
                                        >
                                            +
                                        </button>
                                    </div>
                                    <span className="text-purple-700 font-medium">
                                        out of {branchCount} approvers must complete their review
                                    </span>
                                </div>

                                {/* Visual representation */}
                                <div className="mt-4 flex items-center gap-2">
                                    {[...Array(branchCount)].map((_, i) => (
                                        <div
                                            key={i}
                                            className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all
                                                ${i < (currentConfig.minimumRequired || 1)
                                                    ? 'bg-purple-500 text-white shadow-md'
                                                    : 'bg-gray-200 text-gray-400'}`}
                                        >
                                            {i + 1}
                                        </div>
                                    ))}
                                    <span className="text-xs text-gray-500 ml-2">
                                        = {currentConfig.minimumRequired || 1} required
                                    </span>
                                </div>
                            </div>
                        )}

                        {/* Optional Description */}
                        <div>
                            <label className="text-sm font-semibold text-gray-700 mb-2 block">
                                Add a note (optional)
                            </label>
                            <textarea
                                value={currentConfig.description || ''}
                                onChange={(e) => onUpdate({ ...currentConfig, description: e.target.value })}
                                placeholder="e.g., Requires approval from both Finance and Legal departments"
                                className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:border-purple-300 focus:ring-2 focus:ring-purple-100 outline-none transition-all"
                                rows={2}
                            />
                        </div>

                        {/* Info box */}
                        <div className="flex items-start gap-3 p-4 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-xl text-sm text-purple-700 border border-purple-100">
                            <Sparkles className="w-5 h-5 mt-0.5 flex-shrink-0 text-purple-500" />
                            <div>
                                <strong>Your current setting:</strong>{' '}
                                {currentConfig.completionMode === 'ALL' &&
                                    `All ${branchCount} approvers must complete their review before the workflow continues.`}
                                {currentConfig.completionMode === 'ANY' &&
                                    `The workflow will continue as soon as any one of the ${branchCount} approvers completes their review.`}
                                {currentConfig.completionMode === 'N_OF_M' &&
                                    `The workflow will continue after ${currentConfig.minimumRequired || 1} of the ${branchCount} approvers complete their review.`}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
