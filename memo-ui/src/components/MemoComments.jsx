import React, { useState, useEffect, useCallback } from 'react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Avatar, AvatarFallback } from './ui/avatar';
import { MoreHorizontal, Reply, Trash2, ChevronDown, ChevronUp, Loader2 } from 'lucide-react';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from './ui/dropdown-menu';
import RichTextEditor from './RichTextEditor';
import { MemoApi, CasAdminApi } from '../lib/api';
import { toast } from 'sonner';

/**
 * Extract mentioned user IDs from TipTap HTML content.
 * TipTap renders mentions as <span data-id="userId" data-type="mention">...</span>
 */
function extractMentionIds(htmlContent) {
    if (!htmlContent) return [];
    const matches = htmlContent.match(/data-id="([^"]+)"/g);
    if (!matches) return [];
    return matches.map(m => m.replace('data-id="', '').replace('"', ''));
}

/**
 * Format a timestamp into a relative time string.
 */
function formatRelativeTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMinutes = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMinutes < 1) return 'Just now';
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
}

/**
 * Get initials from a name string.
 */
function getInitials(name) {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
}

/**
 * Single comment component with reply functionality.
 */
function CommentItem({ comment, onReply, onDelete, isReply = false, users, currentUserId }) {
    const [showReplyEditor, setShowReplyEditor] = useState(false);
    const [replyContent, setReplyContent] = useState('');
    const [isPostingReply, setIsPostingReply] = useState(false);
    const [showReplies, setShowReplies] = useState(true);

    const handlePostReply = async () => {
        if (!replyContent || replyContent === '<p><br></p>') return;

        setIsPostingReply(true);
        try {
            const mentionedUserIds = extractMentionIds(replyContent);
            await onReply(comment.id, replyContent, mentionedUserIds);
            setReplyContent('');
            setShowReplyEditor(false);
            toast.success('Reply posted');
        } catch (err) {
            toast.error('Failed to post reply');
        } finally {
            setIsPostingReply(false);
        }
    };

    const isOwnComment = comment.userId === currentUserId;
    const hasReplies = comment.replies && comment.replies.length > 0;

    return (
        <div className={`relative group ${isReply ? 'ml-12' : ''}`}>
            {/* Thread line for parent with replies */}
            {!isReply && hasReplies && (
                <div className="absolute left-[20px] top-12 bottom-0 w-0.5 bg-slate-200 z-0" />
            )}

            <Card className={`border shadow-sm relative z-10 ${isReply ? 'border-slate-150 bg-slate-50/50' : 'border-slate-200'}`}>
                <CardContent className={isReply ? 'p-3' : 'p-5'}>
                    <div className="flex gap-3">
                        <Avatar className={`${isReply ? 'h-8 w-8' : 'h-10 w-10'} border border-slate-200 shadow-sm flex-shrink-0`}>
                            <AvatarFallback className="bg-slate-800 text-white text-xs font-medium">
                                {getInitials(comment.userName)}
                            </AvatarFallback>
                        </Avatar>

                        <div className="flex-1 min-w-0 space-y-1">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2 flex-wrap">
                                    <span className="font-bold text-slate-900 text-sm">{comment.userName || 'Unknown User'}</span>
                                    <span className="text-slate-300">·</span>
                                    <span className="text-xs text-slate-400 font-medium">{formatRelativeTime(comment.createdAt)}</span>
                                    {comment.type && (
                                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${comment.type === 'APPROVAL' ? 'bg-green-100 text-green-700' :
                                                comment.type === 'REJECTION' ? 'bg-red-100 text-red-700' :
                                                    comment.type === 'COMMENT' ? 'bg-slate-100 text-slate-600' :
                                                        'bg-amber-100 text-amber-700'
                                            }`}>
                                            {comment.type}
                                        </span>
                                    )}
                                </div>

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button variant="ghost" size="icon" className="h-7 w-7 text-slate-400 hover:text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <MoreHorizontal className="h-4 w-4" />
                                        </Button>
                                    </DropdownMenuTrigger>
                                    <DropdownMenuContent align="end" className="w-36">
                                        <DropdownMenuItem onClick={() => setShowReplyEditor(!showReplyEditor)}>
                                            <Reply className="h-4 w-4 mr-2" />
                                            Reply
                                        </DropdownMenuItem>
                                        {isOwnComment && (
                                            <DropdownMenuItem onClick={() => onDelete(comment.id)} className="text-red-600 focus:text-red-600">
                                                <Trash2 className="h-4 w-4 mr-2" />
                                                Delete
                                            </DropdownMenuItem>
                                        )}
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>

                            <div
                                className="text-slate-700 text-sm leading-relaxed prose prose-sm max-w-none prose-p:my-1 prose-ul:my-1 prose-ol:my-1 prose-li:my-0 [&_.mention]:bg-blue-100 [&_.mention]:text-blue-700 [&_.mention]:px-1 [&_.mention]:rounded [&_.mention]:font-medium [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5"
                                dangerouslySetInnerHTML={{ __html: comment.content }}
                            />

                            {/* Reply button (inline) */}
                            {!isReply && (
                                <div className="flex items-center gap-3 pt-1">
                                    <button
                                        onClick={() => setShowReplyEditor(!showReplyEditor)}
                                        className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-blue-600 font-medium transition-colors"
                                    >
                                        <Reply className="h-3.5 w-3.5" />
                                        Reply
                                    </button>
                                    {hasReplies && (
                                        <button
                                            onClick={() => setShowReplies(!showReplies)}
                                            className="flex items-center gap-1 text-xs text-slate-400 hover:text-slate-600 font-medium transition-colors"
                                        >
                                            {showReplies ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                                            {comment.replies.length} {comment.replies.length === 1 ? 'reply' : 'replies'}
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Inline reply editor */}
            {showReplyEditor && (
                <div className="ml-12 mt-2 mb-2">
                    <Card className="border border-blue-200 shadow-sm bg-blue-50/30">
                        <CardContent className="p-3">
                            <RichTextEditor
                                content={replyContent}
                                onChange={setReplyContent}
                                placeholder={`Reply to ${comment.userName}...`}
                                minHeight="80px"
                                enableMentions={true}
                                users={users}
                            />
                            <div className="flex justify-end gap-2 mt-2">
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => { setShowReplyEditor(false); setReplyContent(''); }}
                                >
                                    Cancel
                                </Button>
                                <Button
                                    size="sm"
                                    onClick={handlePostReply}
                                    disabled={!replyContent || replyContent === '<p><br></p>' || isPostingReply}
                                    className="bg-blue-600 hover:bg-blue-700 text-white"
                                >
                                    {isPostingReply ? <><Loader2 className="h-3 w-3 mr-1 animate-spin" /> Posting...</> : 'Reply'}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* Replies */}
            {hasReplies && showReplies && (
                <div className="mt-2 space-y-2">
                    {comment.replies.map(reply => (
                        <CommentItem
                            key={reply.id}
                            comment={reply}
                            onReply={onReply}
                            onDelete={onDelete}
                            isReply={true}
                            users={users}
                            currentUserId={currentUserId}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

/**
 * Main memo comments component with threaded replies and @mentions.
 */
export default function MemoComments({ memoId }) {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [isPosting, setIsPosting] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [users, setUsers] = useState([]);
    const [currentUserId, setCurrentUserId] = useState(null);

    // Load comments and users
    useEffect(() => {
        if (!memoId) return;

        const loadData = async () => {
            setIsLoading(true);
            try {
                const [commentsData, usersData] = await Promise.all([
                    MemoApi.getComments(memoId),
                    CasAdminApi.getUsers('MMS').catch(() => [])
                ]);

                setComments(commentsData || []);

                // Transform users for mention suggestions
                // API returns DropdownItem: { id, code, label }
                const mentionUsers = (usersData || []).map(u => ({
                    id: u.id,
                    label: u.label || u.code || 'Unknown',
                    role: u.code || '',
                }));
                setUsers(mentionUsers);

                // Get current user from localStorage
                try {
                    const userStr = localStorage.getItem('user');
                    if (userStr) {
                        const user = JSON.parse(userStr);
                        setCurrentUserId(user.id || user.userId);
                    }
                } catch (e) {
                    // ignore
                }
            } catch (err) {
                console.error('Failed to load comments:', err);
                toast.error('Failed to load comments');
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, [memoId]);

    const handlePostComment = async () => {
        if (!newComment || newComment === '<p><br></p>') return;

        setIsPosting(true);
        try {
            const mentionedUserIds = extractMentionIds(newComment);
            const result = await MemoApi.addComment(memoId, {
                content: newComment,
                mentionedUserIds: mentionedUserIds.length > 0 ? mentionedUserIds : undefined,
            });
            setComments([result, ...comments]);
            setNewComment('');
            toast.success('Comment posted successfully');
        } catch (err) {
            console.error('Failed to post comment:', err);
            toast.error('Failed to post comment');
        } finally {
            setIsPosting(false);
        }
    };

    const handleReply = useCallback(async (parentCommentId, content, mentionedUserIds) => {
        const result = await MemoApi.addComment(memoId, {
            content,
            parentCommentId,
            mentionedUserIds: mentionedUserIds?.length > 0 ? mentionedUserIds : undefined,
        });

        // Insert reply into the right parent
        setComments(prev => prev.map(comment => {
            if (comment.id === parentCommentId) {
                return {
                    ...comment,
                    replies: [...(comment.replies || []), result],
                };
            }
            return comment;
        }));
    }, [memoId]);

    const handleDelete = useCallback(async (commentId) => {
        try {
            await MemoApi.deleteComment(memoId, commentId);
            // Remove from top-level or from replies
            setComments(prev =>
                prev
                    .filter(c => c.id !== commentId)
                    .map(c => ({
                        ...c,
                        replies: (c.replies || []).filter(r => r.id !== commentId),
                    }))
            );
            toast.success('Comment deleted');
        } catch (err) {
            console.error('Failed to delete comment:', err);
            toast.error('Failed to delete comment');
        }
    }, [memoId]);

    return (
        <div className="max-w-4xl mx-auto space-y-6">
            {/* Input Area */}
            <Card className="border shadow-sm bg-white overflow-hidden">
                <CardContent className="p-0">
                    <div className="p-4">
                        <RichTextEditor
                            content={newComment}
                            onChange={setNewComment}
                            placeholder="Write your comment... Use @ to tag people"
                            minHeight="100px"
                            enableMentions={true}
                            users={users}
                        />
                    </div>
                    <div className="bg-slate-50 px-4 py-3 border-t border-slate-100 flex items-center justify-between">
                        <span className="text-xs text-slate-400">
                            Type <kbd className="px-1.5 py-0.5 bg-slate-200 rounded text-slate-600 font-mono text-[10px]">@</kbd> to mention someone
                        </span>
                        <Button
                            onClick={handlePostComment}
                            disabled={!newComment || newComment === '<p><br></p>' || isPosting}
                            className="bg-brand-blue hover:bg-brand-blue-hover text-white shadow-sm"
                        >
                            {isPosting ? <><Loader2 className="h-4 w-4 mr-2 animate-spin" /> Posting...</> : 'Post Comment'}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* Loading State */}
            {isLoading && (
                <div className="flex items-center justify-center py-12">
                    <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                    <span className="ml-2 text-slate-400 text-sm">Loading comments...</span>
                </div>
            )}

            {/* Empty State */}
            {!isLoading && comments.length === 0 && (
                <div className="text-center py-12">
                    <div className="text-slate-300 text-4xl mb-3">💬</div>
                    <p className="text-slate-400 text-sm font-medium">No comments yet</p>
                    <p className="text-slate-300 text-xs mt-1">Be the first to add a comment</p>
                </div>
            )}

            {/* Comments List */}
            {!isLoading && (
                <div className="space-y-4">
                    {comments.map((comment) => (
                        <CommentItem
                            key={comment.id}
                            comment={comment}
                            onReply={handleReply}
                            onDelete={handleDelete}
                            users={users}
                            currentUserId={currentUserId}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
