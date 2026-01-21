import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import RichTextEditor from '../components/RichTextEditor';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Save, Send, ArrowLeft, FileIcon, Download, Loader2 } from 'lucide-react';
import FileUploader from '../components/FileUploader';
import { toast } from 'sonner';

export default function MemoEditor() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [memo, setMemo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [attachments, setAttachments] = useState([]);

    useEffect(() => {
        loadMemo();
        loadAttachments();
    }, [id]);

    const loadMemo = async () => {
        try {
            const data = await MemoApi.getMemo(id);
            setMemo(data);
        } catch (error) {
            console.error(error);
            toast.error("Failed to load memo");
        } finally {
            setLoading(false);
        }
    };

    const loadAttachments = async () => {
        try {
            const data = await MemoApi.getAttachments(id);
            setAttachments(data);
        } catch (error) {
            console.error("Failed to load attachments", error);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await MemoApi.updateMemo(id, {
                subject: memo.subject,
                priority: memo.priority,
                content: memo.content,
                formData: memo.formData
            });
            toast.success("Memo saved successfully");
        } catch (error) {
            console.error(error);
            toast.error("Failed to save memo");
        } finally {
            setSaving(false);
        }
    };

    if (loading || !memo) return <div className="flex h-screen items-center justify-center"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div>;

    return (
        <div className="max-w-6xl mx-auto space-y-6 pb-20 animate-in fade-in duration-300">
            {/* Header */}
            <div className="flex items-center justify-between sticky top-0 bg-background/95 backdrop-blur z-20 py-4 border-b border-border/40">
                <div className="flex items-center gap-4">
                    <Button variant="ghost" size="icon" onClick={() => navigate('/')}>
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <div>
                        <div className="flex items-center gap-2">
                            <div className="font-mono text-xs text-muted-foreground uppercase tracking-wider">{memo.memoNumber}</div>
                            <Badge variant={memo.status === 'APPROVED' ? 'success' : 'secondary'} className="text-[10px] px-1.5 py-0 h-5">
                                {memo.status}
                            </Badge>
                        </div>
                        <input
                            type="text"
                            value={memo.subject}
                            onChange={(e) => setMemo({ ...memo, subject: e.target.value })}
                            className="text-xl font-bold bg-transparent border-none focus:outline-none focus:ring-0 p-0 w-[400px] placeholder:text-muted-foreground/50"
                            placeholder="Enter subject..."
                        />
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <select
                        value={memo.priority}
                        onChange={(e) => setMemo({ ...memo, priority: e.target.value })}
                        className="h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    >
                        <option value="NORMAL">Normal Priority</option>
                        <option value="HIGH">High Priority</option>
                        <option value="URGENT">Urgent Priority</option>
                    </select>
                    <div className="h-6 w-px bg-border mx-1" />
                    <Button variant="outline" onClick={handleSave} disabled={saving} size="sm">
                        {saving ? <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" /> : <Save className="mr-2 h-3.5 w-3.5" />}
                        Save
                    </Button>
                    <Button size="sm" onClick={() => toast.info('Submit workflow coming soon')}>
                        <Send className="mr-2 h-3.5 w-3.5" /> Submit
                    </Button>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                {/* Main Content (Rich Text) */}
                <div className="lg:col-span-8 space-y-6">
                    <Card className="min-h-[500px] shadow-sm">
                        <CardContent className="p-0">
                            <RichTextEditor
                                content={memo.content}
                                onChange={(content) => setMemo({ ...memo, content })}
                            />
                        </CardContent>
                    </Card>

                    {/* Attachments Section */}
                    <Card>
                        <CardHeader className="pb-3 border-b">
                            <div className="flex items-center justify-between">
                                <CardTitle className="text-base font-medium">Attachments</CardTitle>
                                <FileUploader memoId={id} onUploadComplete={loadAttachments} />
                            </div>
                        </CardHeader>
                        <CardContent className="pt-4">
                            {attachments.length === 0 ? (
                                <div className="flex flex-col items-center justify-center p-8 border-2 border-dashed rounded-lg bg-muted/10 text-muted-foreground">
                                    <FileIcon className="h-8 w-8 mb-2 opacity-20" />
                                    <p className="text-sm">No files attached yet</p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                    {attachments.map(file => (
                                        <div key={file.id} className="flex items-center justify-between p-3 border rounded-lg bg-card hover:bg-muted/30 transition-all group">
                                            <div className="flex items-center gap-3 overflow-hidden">
                                                <div className="bg-primary/10 p-2.5 rounded-md text-primary">
                                                    <FileIcon className="h-4 w-4" />
                                                </div>
                                                <div className="truncate">
                                                    <div className="text-sm font-medium truncate text-foreground">{file.fileName}</div>
                                                    <div className="text-xs text-muted-foreground">{(file.size / 1024).toFixed(1)} KB</div>
                                                </div>
                                            </div>
                                            <a href={MemoApi.getAttachmentUrl(id, file.id)} target="_blank" rel="noreferrer" className="opacity-0 group-hover:opacity-100 transition-opacity">
                                                <Button variant="ghost" size="icon" className="h-8 w-8">
                                                    <Download className="h-4 w-4 text-muted-foreground" />
                                                </Button>
                                            </a>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Sidebar (Details & Forms) */}
                <div className="lg:col-span-4 space-y-6">
                    <Card>
                        <CardHeader className="pb-3 border-b bg-muted/20">
                            <CardTitle className="text-sm font-medium uppercase tracking-wide text-muted-foreground">Memo Details</CardTitle>
                        </CardHeader>
                        <CardContent className="pt-4 space-y-4 text-sm">
                            <div className="flex justify-between items-center py-1">
                                <span className="text-muted-foreground">Category</span>
                                <span className="font-medium text-right">{memo.categoryName}</span>
                            </div>
                            <div className="flex justify-between items-center py-1 border-t border-dashed pt-4">
                                <span className="text-muted-foreground">Topic</span>
                                <Badge variant="outline" className="font-normal">{memo.topicName}</Badge>
                            </div>
                            <div className="flex justify-between items-center py-1 border-t border-dashed pt-4">
                                <span className="text-muted-foreground">Status</span>
                                <span className="font-medium">{memo.status}</span>
                            </div>
                            <div className="flex justify-between items-center py-1 border-t border-dashed pt-4">
                                <span className="text-muted-foreground">Created</span>
                                <span>{memo.createdAt ? new Date(memo.createdAt).toLocaleDateString() : '-'}</span>
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader className="pb-3 border-b bg-muted/20">
                            <CardTitle className="text-sm font-medium uppercase tracking-wide text-muted-foreground">Additional Data</CardTitle>
                        </CardHeader>
                        <CardContent className="pt-4 text-sm text-muted-foreground">
                            <p>Dynamic form fields will appear here based on the selected topic.</p>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}
