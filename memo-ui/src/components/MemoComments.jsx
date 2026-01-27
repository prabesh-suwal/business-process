import React, { useState } from 'react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { MoreHorizontal } from 'lucide-react';
import RichTextEditor from './RichTextEditor';
import { toast } from 'sonner';

const MOCK_COMMENTS = [
    {
        id: 1,
        author: "Michael Chen",
        role: "Senior Risk Officer",
        avatar: "MC",
        time: "2 hours ago",
        content: "<p>I have reviewed the proposed facilities. While the Tier 1 Strategic allocation looks solid, I have concerns about the exposure to the renewable sector given recent market volatility. Please provide more detailed risk mitigation strategies for this specific segment.</p>",
        hasThreadLine: true
    },
    {
        id: 2,
        author: "Sarah Jenkins",
        role: "Credit Analyst",
        avatar: "SJ",
        time: "2 hours ago",
        content: "<p>Understood, Michael. I will prepare an addendum addressing the renewable sector risk and circulate it by EOD tomorrow.</p>",
        hasThreadLine: false
    },
    {
        id: 3,
        author: "David Lee",
        role: "Portfolio Manager",
        avatar: "DL",
        time: "2 hours ago",
        content: "<p>The compliance analysis looks good. Ensure all KYC/AML checks are finalized before approval.</p>",
        hasThreadLine: false
    }
];

export default function MemoComments({ memoId }) {
    const [comments, setComments] = useState(MOCK_COMMENTS);
    const [newComment, setNewComment] = useState('');
    const [isPosting, setIsPosting] = useState(false);

    const handlePostComment = () => {
        if (!newComment || newComment === '<p><br></p>') return;

        setIsPosting(true);

        // Simulate API call
        setTimeout(() => {
            const newCommentObj = {
                id: comments.length + 1,
                author: "You",
                role: "Current User",
                avatar: "ME",
                time: "Just now",
                content: newComment,
                hasThreadLine: false
            };

            setComments([newCommentObj, ...comments]);
            setNewComment('');
            setIsPosting(false);
            toast.success("Comment posted successfully");
        }, 600);
    };

    return (
        <div className="max-w-4xl mx-auto space-y-8">
            {/* Input Area */}
            <Card className="border shadow-sm bg-white overflow-hidden">
                <CardContent className="p-0">
                    <div className="p-4">
                        <RichTextEditor
                            content={newComment}
                            onChange={setNewComment}
                            placeholder="Write your comment..."
                            className="min-h-[120px] prose-sm"
                        />
                    </div>
                    <div className="bg-slate-50 px-4 py-3 border-t border-slate-100 flex justify-end">
                        <Button
                            onClick={handlePostComment}
                            disabled={!newComment || isPosting}
                            className="bg-brand-blue hover:bg-brand-blue-hover text-white shadow-sm"
                        >
                            {isPosting ? 'Posting...' : 'Post Comment'}
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* Comments List */}
            <div className="space-y-4">
                {comments.map((comment, index) => (
                    <div key={comment.id} className="relative group">
                        {/* Thread Line Visualization */}
                        {comment.hasThreadLine && (
                            <div className="absolute left-[26px] top-12 bottom-[-24px] w-0.5 bg-slate-200 z-0"></div>
                        )}

                        <Card className="border border-slate-200 shadow-sm relative z-10">
                            <CardContent className="p-6">
                                <div className="flex gap-4">
                                    <Avatar className="h-10 w-10 border border-slate-200 shadow-sm">
                                        <AvatarFallback className="bg-slate-800 text-white text-xs font-medium">
                                            {comment.avatar}
                                        </AvatarFallback>
                                    </Avatar>

                                    <div className="flex-1 space-y-1">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <span className="font-bold text-slate-900">{comment.author}</span>
                                                <span className="text-slate-400 text-sm">-</span>
                                                <span className="font-medium text-slate-700 text-sm">{comment.role}</span>
                                            </div>
                                            <Button variant="ghost" size="icon" className="h-6 w-6 text-slate-400 hover:text-slate-600">
                                                <MoreHorizontal className="h-4 w-4" />
                                            </Button>
                                        </div>
                                        <div className="text-xs text-slate-400 font-medium mb-3">{comment.time}</div>

                                        <div
                                            className="text-slate-700 text-sm leading-relaxed prose prose-sm max-w-none prose-p:my-1"
                                            dangerouslySetInnerHTML={{ __html: comment.content }}
                                        />
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    </div>
                ))}
            </div>
        </div>
    );
}
