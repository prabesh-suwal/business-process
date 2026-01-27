import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { toast } from 'sonner';
import { ArrowLeft, Eye, FileText, Calendar, User, Lock } from 'lucide-react';
import { PageContainer } from '../components/PageContainer';

/**
 * ViewOnlyMemoDetail - Read-only view of a memo
 * Shows memo content and workflow history without action buttons
 */
const ViewOnlyMemoDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [memo, setMemo] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadMemo();
    }, [id]);

    const loadMemo = async () => {
        try {
            setLoading(true);
            const data = await MemoApi.getMemo(id);
            setMemo(data);
        } catch (error) {
            console.error('Error loading memo:', error);
            toast.error('Failed to load memo');
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
        return date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <div className="p-6">
                <div className="animate-pulse space-y-4">
                    <div className="h-8 bg-gray-200 rounded w-1/4"></div>
                    <div className="h-64 bg-gray-200 rounded"></div>
                </div>
            </div>
        );
    }

    if (!memo) {
        return (
            <div className="p-6">
                <Card>
                    <CardContent className="flex flex-col items-center justify-center py-12">
                        <FileText className="w-12 h-12 text-gray-400 mb-4" />
                        <p className="text-gray-600">Memo not found</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <PageContainer>
            {/* Header with back button and view-only banner */}
            <div className="flex items-center justify-between">
                <Button
                    variant="ghost"
                    onClick={() => navigate('/view-only-memos')}
                    className="gap-2"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Back to View-Only Memos
                </Button>
                <Badge variant="outline" className="text-blue-600 border-blue-600">
                    <Eye className="w-4 h-4 mr-1" />
                    Read Only Access
                </Badge>
            </div>

            {/* View-Only Notice */}
            <Card className="border-blue-200 bg-blue-50">
                <CardContent className="flex items-center gap-3 py-3">
                    <Lock className="w-5 h-5 text-blue-600" />
                    <div>
                        <p className="text-sm font-medium text-blue-900">View-Only Mode</p>
                        <p className="text-xs text-blue-700">
                            You have permission to view this memo but cannot make changes or take actions.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Memo Details Card */}
            <Card>
                <CardHeader>
                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                                <span className="text-sm font-mono text-gray-500">
                                    {memo.memoNumber}
                                </span>
                                <Badge className={getStatusColor(memo.status)}>
                                    {memo.status}
                                </Badge>
                            </div>
                            <CardTitle className="text-2xl">{memo.subject}</CardTitle>
                        </div>
                    </div>
                </CardHeader>
                <CardContent className="space-y-6">
                    {/* Metadata */}
                    <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded-lg">
                        <div className="flex items-center gap-2 text-sm">
                            <FileText className="w-4 h-4 text-gray-500" />
                            <div>
                                <div className="text-xs text-gray-500">Topic</div>
                                <div className="font-medium">{memo.topicName}</div>
                            </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <FileText className="w-4 h-4 text-gray-500" />
                            <div>
                                <div className="text-xs text-gray-500">Category</div>
                                <div className="font-medium">{memo.categoryName}</div>
                            </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <Calendar className="w-4 h-4 text-gray-500" />
                            <div>
                                <div className="text-xs text-gray-500">Created</div>
                                <div className="font-medium">{formatDate(memo.createdAt)}</div>
                            </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <User className="w-4 h-4 text-gray-500" />
                            <div>
                                <div className="text-xs text-gray-500">Creator</div>
                                <div className="font-medium text-xs">{memo.createdBy}</div>
                            </div>
                        </div>
                    </div>

                    {/* Content */}
                    <div>
                        <h3 className="text-sm font-semibold text-gray-700 mb-2">Content</h3>
                        <div className="prose prose-sm max-w-none p-4 bg-gray-50 rounded-lg">
                            {memo.content ? (
                                <div dangerouslySetInnerHTML={{ __html: memo.content }} />
                            ) : (
                                <p className="text-gray-500 italic">No content</p>
                            )}
                        </div>
                    </div>

                    {/* Form Data (if any) */}
                    {memo.formData && Object.keys(memo.formData).length > 0 && (
                        <div>
                            <h3 className="text-sm font-semibold text-gray-700 mb-2">Form Data</h3>
                            <div className="p-4 bg-gray-50 rounded-lg">
                                <pre className="text-xs text-gray-700 overflow-auto">
                                    {JSON.stringify(memo.formData, null, 2)}
                                </pre>
                            </div>
                        </div>
                    )}

                    {/* Workflow Status (placeholder - can be enhanced with actual task history) */}
                    <div>
                        <h3 className="text-sm font-semibold text-gray-700 mb-2">Workflow Status</h3>
                        <div className="p-4 bg-gray-50 rounded-lg">
                            <div className="flex items-center gap-2">
                                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                                <span className="text-sm text-gray-700">
                                    Current Stage: {memo.currentStage || 'Unknown'}
                                </span>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </PageContainer>
    );
};

export default ViewOnlyMemoDetail;
