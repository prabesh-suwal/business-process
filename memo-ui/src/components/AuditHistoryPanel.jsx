import React, { useState, useEffect } from 'react';
import { HistoryApi } from '../lib/api';
import {
    Clock,
    CheckCircle,
    XCircle,
    CornerUpLeft,
    FileText,
    User,
    ChevronDown,
    ChevronRight,
    MessageSquare,
    Play
} from 'lucide-react';
import { format } from 'date-fns';

const AuditHistoryPanel = ({ processInstanceId, className = "" }) => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expanded, setExpanded] = useState(false);

    useEffect(() => {
        if (processInstanceId) {
            loadHistory();
        }
    }, [processInstanceId]);

    const loadHistory = async () => {
        try {
            setLoading(true);
            const data = await HistoryApi.getTimeline(processInstanceId);
            setEvents(data);
        } catch (error) {
            console.error('Failed to load history:', error);
        } finally {
            setLoading(false);
        }
    };

    const getIcon = (type) => {
        switch (type) {
            case 'PROCESS_STARTED': return <Play className="w-4 h-4 text-blue-500" />;
            case 'PROCESS_COMPLETED': return <CheckCircle className="w-4 h-4 text-green-500" />;
            case 'TASK_COMPLETED': return <CheckCircle className="w-4 h-4 text-green-600" />;
            case 'TASK_REJECTED': return <XCircle className="w-4 h-4 text-red-500" />;
            case 'TASK_SENT_BACK': return <CornerUpLeft className="w-4 h-4 text-orange-500" />;
            case 'COMMENT_ADDED': return <MessageSquare className="w-4 h-4 text-gray-500" />;
            default: return <Clock className="w-4 h-4 text-gray-400" />;
        }
    };

    const getActionColor = (type) => {
        if (type.includes('REJECT') || type.includes('BACK')) return 'bg-red-50 border-red-100';
        if (type.includes('COMPLETED') || type.includes('APPROVED')) return 'bg-green-50 border-green-100';
        return 'bg-white border-gray-100';
    };

    if (!processInstanceId) return null;

    return (
        <div className={`border rounded-lg bg-white shadow-sm ${className}`}>
            <button
                onClick={() => setExpanded(!expanded)}
                className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 transition-colors rounded-t-lg"
            >
                <div className="flex items-center gap-2">
                    <Clock className="w-5 h-5 text-gray-600" />
                    <h3 className="font-semibold text-gray-800">Audit History</h3>
                    <span className="bg-gray-200 text-gray-600 text-xs px-2 py-0.5 rounded-full">
                        {events.length}
                    </span>
                </div>
                {expanded ? <ChevronDown className="w-5 h-5 text-gray-500" /> : <ChevronRight className="w-5 h-5 text-gray-500" />}
            </button>

            {expanded && (
                <div className="p-4 max-h-[500px] overflow-y-auto">
                    {loading ? (
                        <div className="animate-pulse space-y-4">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="h-16 bg-gray-100 rounded"></div>
                            ))}
                        </div>
                    ) : (
                        <div className="relative space-y-6 pl-4">
                            {/* Vertical Line */}
                            <div className="absolute left-[23px] top-2 bottom-2 w-0.5 bg-gray-200"></div>

                            {events.map((event, index) => (
                                <div key={event.id} className="relative flex gap-4">
                                    {/* Icon Bubble */}
                                    <div className={`relative z-10 flex items-center justify-center w-8 h-8 rounded-full border bg-white shadow-sm shrink-0`}>
                                        {getIcon(event.actionType)}
                                    </div>

                                    {/* Content Card */}
                                    <div className={`flex-1 p-3 rounded-lg border ${getActionColor(event.actionType)}`}>
                                        <div className="flex justify-between items-start mb-1">
                                            <span className="text-sm font-medium text-gray-900">
                                                {event.description}
                                            </span>
                                            <span className="text-xs text-gray-500 whitespace-nowrap ml-2">
                                                {format(new Date(event.createdAt), 'MMM d, h:mm a')}
                                            </span>
                                        </div>

                                        {event.taskName && (
                                            <div className="text-xs text-blue-600 mb-1">
                                                Step: {event.taskName}
                                            </div>
                                        )}

                                        {event.metadata?.reason && (
                                            <div className="mt-2 text-sm text-gray-700 bg-white/50 p-2 rounded border border-gray-100 italic">
                                                "{event.metadata.reason}"
                                            </div>
                                        )}

                                        {event.metadata?.comment && (
                                            <div className="mt-2 text-sm text-gray-700 bg-white/50 p-2 rounded border border-gray-100">
                                                {event.metadata.comment}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}

                            {events.length === 0 && (
                                <div className="text-center text-gray-500 py-4">No history available</div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default AuditHistoryPanel;
