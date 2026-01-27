import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { PageContainer } from '../components/PageContainer';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import RichTextEditor from '../components/RichTextEditor';
import MemoAttachments from '../components/MemoAttachments';
import { Loader2, Save, Send } from 'lucide-react';
import { toast } from 'sonner';

export default function CreateMemo() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('content');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    // Form State
    const [title, setTitle] = useState('');
    const [referenceNumber, setReferenceNumber] = useState('');
    const [priority, setPriority] = useState('NORMAL');
    const [content, setContent] = useState('');

    // Selection State
    const [categories, setCategories] = useState([]);
    const [topics, setTopics] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState('');
    const [selectedTopic, setSelectedTopic] = useState('');

    // Created Memo
    const [memoId, setMemoId] = useState(null);

    // Fetch Categories on load
    useEffect(() => {
        setLoading(true);
        MemoApi.getCategories()
            .then(setCategories)
            .catch(err => {
                console.error(err);
                toast.error("Failed to load categories");
            })
            .finally(() => setLoading(false));
    }, []);

    // Fetch Topics when Category changes
    useEffect(() => {
        if (selectedCategory) {
            MemoApi.getTopics(selectedCategory)
                .then(setTopics)
                .catch(err => {
                    console.error(err);
                    toast.error("Failed to load topics");
                });
        } else {
            setTopics([]);
        }
    }, [selectedCategory]);

    const handleSaveDraft = async () => {
        if (!selectedTopic) {
            toast.error("Please select a topic first.");
            return;
        }
        if (!title.trim()) {
            toast.error("Please enter a title.");
            return;
        }

        setSaving(true);
        try {
            let currentMemoId = memoId;

            // 1. Create Draft if doesn't exist
            if (!currentMemoId) {
                const draft = await MemoApi.createDraft({
                    topicId: selectedTopic,
                    subject: title,
                    priority: priority
                });
                currentMemoId = draft.id;
                setMemoId(currentMemoId);
                toast.success("Draft created successfully");
            }

            // 2. Update Content & Metadata
            // Assuming updateMemo accepts these fields. 
            // If referenceNumber needs to go into formData, we might need to change structure.
            // For now, sending flat.
            await MemoApi.updateMemo(currentMemoId, {
                subject: title,
                priority: priority,
                content: content, // Rich text HTML
                referenceNumber: referenceNumber
            });

            toast.success("Memo saved");

            // Optional: Navigate to edit page if create page is distinct?
            // "The page we used for viewing memo is a different."
            // "Create memo should be something like this."
            // I'll stay on this page to allow Attachments upload after save.

        } catch (error) {
            console.error("Error saving memo:", error);
            toast.error("Failed to save memo");
        } finally {
            setSaving(false);
        }
    };

    const handleSubmit = async () => {
        if (!memoId) {
            // Try saving first
            await handleSaveDraft();
            if (!memoId) return; // Save failed
        }

        try {
            await MemoApi.submitMemo(memoId);
            toast.success("Memo submitted successfully");
            navigate('/dashboard'); // or wherever appropriate
        } catch (error) {
            console.error(error);
            toast.error("Failed to submit memo");
        }
    };

    return (
        <PageContainer>
            <div className="mb-6">
                <h1 className="text-3xl font-bold tracking-tight">Create New Memo</h1>
                <p className="text-muted-foreground mt-2">Complete the tabbed sections below to submit your corporate memo.</p>
            </div>

            <div className="bg-background rounded-lg border shadow-sm">
                <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
                    <div className="border-b px-4 bg-muted/20">
                        <TabsList className="bg-transparent h-14 w-full justify-start space-x-6 p-0">
                            <TabsTrigger
                                value="content"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Content
                            </TabsTrigger>
                            <TabsTrigger
                                value="attachments"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Attachments
                            </TabsTrigger>
                            <TabsTrigger
                                value="workflow"
                                className="data-[state=active]:bg-transparent data-[state=active]:shadow-none data-[state=active]:border-b-2 data-[state=active]:border-primary data-[state=active]:text-primary rounded-none px-2 h-full font-semibold"
                            >
                                Workflow
                            </TabsTrigger>
                        </TabsList>
                    </div>

                    <div className="p-6">
                        <TabsContent value="content" className="space-y-6 mt-0">
                            {/* Title */}
                            <div>
                                <Label htmlFor="title" className="text-sm font-semibold mb-1.5 block">Memo Title</Label>
                                <Input
                                    id="title"
                                    placeholder="e.g., Credit Facility Revision - Enterprise Client Corp"
                                    className="h-12 text-lg"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                />
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {/* Category & Topic Selection (Added per requirement) */}
                                <div>
                                    <Label htmlFor="category" className="text-sm font-semibold mb-1.5 block">Category</Label>
                                    <select
                                        id="category"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={selectedCategory}
                                        onChange={(e) => setSelectedCategory(e.target.value)}
                                        disabled={!!memoId} // Disable if already created? Maybe allow changing if draft?
                                    >
                                        <option value="">Select Category</option>
                                        {categories.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <Label htmlFor="topic" className="text-sm font-semibold mb-1.5 block">Topic</Label>
                                    <select
                                        id="topic"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={selectedTopic}
                                        onChange={(e) => setSelectedTopic(e.target.value)}
                                        disabled={!selectedCategory || !!memoId}
                                    >
                                        <option value="">Select Topic</option>
                                        {topics.map(topic => (
                                            <option key={topic.id} value={topic.id}>{topic.name}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Ref & Priority */}
                                <div>
                                    <Label htmlFor="ref" className="text-sm font-semibold mb-1.5 block">Reference Number</Label>
                                    <Input
                                        id="ref"
                                        placeholder="REF-2023-KB82"
                                        value={referenceNumber}
                                        onChange={(e) => setReferenceNumber(e.target.value)}
                                    />
                                </div>
                                <div>
                                    <Label htmlFor="priority" className="text-sm font-semibold mb-1.5 block">Priority Level</Label>
                                    <select
                                        id="priority"
                                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                        value={priority}
                                        onChange={(e) => setPriority(e.target.value)}
                                    >
                                        <option value="NORMAL">Standard</option>
                                        <option value="HIGH">High</option>
                                        <option value="URGENT">Urgent</option>
                                    </select>
                                </div>
                            </div>

                            {/* Editor */}
                            <div>
                                <div className="flex justify-between items-center mb-1.5">
                                    <Label className="text-sm font-semibold">Executive Summary</Label>
                                    <span className="text-xs text-muted-foreground">Formal document editor</span>
                                </div>
                                <RichTextEditor
                                    content={content}
                                    onChange={setContent}
                                    outputFormat="json"
                                />
                            </div>
                        </TabsContent>

                        <TabsContent value="attachments" className="mt-0">
                            {memoId ? (
                                <MemoAttachments
                                    memoId={memoId}
                                    isEditable={true}
                                    onUpload={() => { }} // Refresh?
                                />
                            ) : (
                                <div className="text-center py-12 text-muted-foreground bg-muted/10 rounded-lg border border-dashed">
                                    <p>Please save the draft first to add attachments.</p>
                                    <Button variant="outline" onClick={handleSaveDraft} className="mt-4">Save Draft</Button>
                                </div>
                            )}
                        </TabsContent>

                        <TabsContent value="workflow" className="mt-0">
                            <div className="p-8 text-center text-muted-foreground">
                                <p>Workflow configuration will be available after selecting a topic and saving the draft.</p>
                            </div>
                        </TabsContent>
                    </div>

                    {/* Footer Actions */}
                    <div className="border-t p-6 bg-muted/10 flex justify-end gap-3 rounded-b-lg">
                        <Button variant="outline" size="lg" onClick={handleSaveDraft} disabled={saving}>
                            {saving ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
                            Save Draft
                        </Button>
                        <Button size="lg" onClick={handleSubmit} disabled={saving || !memoId} className="bg-primary hover:bg-primary/90">
                            Submit Memo
                            <Send className="w-4 h-4 ml-2" />
                        </Button>
                    </div>
                </Tabs>
            </div>
        </PageContainer>
    );
}
