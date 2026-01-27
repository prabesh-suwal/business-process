import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { toast } from 'sonner';
import { Eye, FileText, Calendar, User } from 'lucide-react';
import { PageContainer } from '../components/PageContainer';

/**
 * ViewOnlyMemos - Display memos that user can view but not act on
 */
const ViewOnlyMemos = () => {
    const [memos, setMemos] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        loadViewableMemos();
    }, []);

    const loadViewableMemos = async () => {
        try {
            setLoading(true);
            const data = await MemoApi.getViewableMemos();
            setMemos(data);
        } catch (error) {
            console.error('Error loading viewable memos:', error);
            toast.error('Failed to load viewable memos');
        } finally {
            setLoading(false);
        }
    };

    const getStatusColor = (status) => {
        const colors = {
            DRAFT: 'bg-gray-100 text-gray-800',
            SUBMITTED: 'bg-blue-100 text-blue-800',
            IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
            APPROVED: 'bg-green-100 text-green-800',
            REJECTED: 'bg-red-100 text-red-800',
        };
        return colors[status] || 'bg-gray-100 text-gray-800';
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    if (loading) {
        return (
            <div className="p-6">
                <div className="animate-pulse space-y-4">
                    <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                    <div className="h-32 bg-gray-200 rounded"></div>
                    <div className="h-32 bg-gray-200 rounded"></div>
                </div>
            </div>
        );
    }


    return (
        <PageContainer>
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">View-Only Memos</h1>
                    <p className="text-sm text-gray-600 mt-1">
                        Memos you have permission to view
                    </p>
                </div>
                <Badge variant="outline" className="text-blue-600 border-blue-600">
                    <Eye className="w-4 h-4 mr-1" />
                    Read Only
                </Badge>
            </div>

            {memos.length === 0 ? (
                <Card>
                    <CardContent className="flex flex-col items-center justify-center py-12">
                        <FileText className="w-12 h-12 text-gray-400 mb-4" />
                        <p className="text-gray-600">No viewable memos found</p>
                        <p className="text-sm text-gray-500 mt-1">
                            Memos will appear here when you're granted view-only access
                        </p>
                    </CardContent>
                </Card>
            ) : (
                <div className="grid gap-4">
                    {memos.map((memo) => (
                        <Card
                            key={memo.id}
                            className="hover:shadow-md transition-shadow cursor-pointer"
                            onClick={() => navigate(`/memos/${memo.id}/view-only`)}
                        >
                            <CardHeader>
                                <div className="flex items-start justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-mono text-gray-500">
                                                {memo.memoNumber}
                                            </span>
                                            <Badge className={getStatusColor(memo.status)}>
                                                {memo.status}
                                            </Badge>
                                            <Badge variant="outline" className="text-xs">
                                                View Only
                                            </Badge>
                                        </div>
                                        <CardTitle className="text-lg">{memo.subject}</CardTitle>
                                    </div>
                                </div>
                            </CardHeader>
                            <CardContent>
                                <div className="grid grid-cols-3 gap-4 text-sm">
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <FileText className="w-4 h-4" />
                                        <div>
                                            <div className="text-xs text-gray-500">Topic</div>
                                            <div className="font-medium">{memo.topicName}</div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <Calendar className="w-4 h-4" />
                                        <div>
                                            <div className="text-xs text-gray-500">Created</div>
                                            <div className="font-medium">{formatDate(memo.createdAt)}</div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <User className="w-4 h-4" />
                                        <div>
                                            <div className="text-xs text-gray-500">Creator</div>
                                            <div className="font-medium text-xs">{memo.createdBy}</div>
                                        </div>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </PageContainer>
    );
};

export default ViewOnlyMemos;
