import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '../components/ui/dialog';
import { Loader2, Plus, ArrowLeft, Settings as SettingsIcon, Workflow, FolderOpen, FileText, Layers, GitBranch } from 'lucide-react';
import { toast } from 'sonner';
import { PageContainer } from '../components/PageContainer';

export default function Settings() {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchCategories = async () => {
        try {
            const data = await MemoApi.getCategories();
            setCategories(data);
        } catch (error) {
            toast.error("Failed to load categories");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

    return (
        <PageContainer className="p-0">
            {/* Premium Dark Header */}
            <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 px-8 py-6">
                <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                        <SettingsIcon className="h-6 w-6 text-white" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-white">System Settings</h1>
                        <p className="text-slate-400 text-sm">Manage categories, topics, and workflow configurations</p>
                    </div>
                </div>
            </div>

            <div className="p-6 md:p-8">
                <Tabs defaultValue="categories" className="space-y-6">
                    <TabsList className="bg-white border border-slate-200 p-1 rounded-xl shadow-sm">
                        <TabsTrigger
                            value="categories"
                            className="rounded-lg px-4 py-2 data-[state=active]:bg-slate-900 data-[state=active]:text-white data-[state=active]:shadow-sm transition-all"
                        >
                            <FolderOpen className="w-4 h-4 mr-2" />
                            Categories
                        </TabsTrigger>
                        <TabsTrigger
                            value="topics"
                            className="rounded-lg px-4 py-2 data-[state=active]:bg-slate-900 data-[state=active]:text-white data-[state=active]:shadow-sm transition-all"
                        >
                            <FileText className="w-4 h-4 mr-2" />
                            Topics
                        </TabsTrigger>
                        <TabsTrigger
                            value="workflows"
                            className="rounded-lg px-4 py-2 data-[state=active]:bg-slate-900 data-[state=active]:text-white data-[state=active]:shadow-sm transition-all"
                        >
                            <GitBranch className="w-4 h-4 mr-2" />
                            Workflows
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="categories" className="space-y-4">
                        <CategoriesTab categories={categories} onRefresh={fetchCategories} />
                    </TabsContent>

                    <TabsContent value="topics" className="space-y-4">
                        <TopicsTab categories={categories} />
                    </TabsContent>

                    <TabsContent value="workflows" className="space-y-4">
                        <WorkflowsTab />
                    </TabsContent>
                </Tabs>
            </div>
        </PageContainer>
    );
}

import BpmnDesigner from '../components/BpmnDesigner';
import { WorkflowApi } from '../lib/api';

function WorkflowsTab() {
    const [workflows, setWorkflows] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedWorkflow, setSelectedWorkflow] = useState(null); // For editing
    const [isDesignerOpen, setIsDesignerOpen] = useState(false);

    // Create New Logic
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [newWorkflow, setNewWorkflow] = useState({ key: '', name: '', description: '', productId: '00000000-0000-0000-0000-000000000000' }); // Default product ID for MVP? Or we fetch products.
    // Assuming product ID is NOT required for now or handled by backend if missing (it might be required). 
    // Wait, Workflow Service requires Product ID. Let's hardcode a "Memo Product ID" or handle it. 
    // Actually, let's assume the user selects a product or we use a default.
    // For now, I will skip product ID input and hope backend handles it or I'll check DTO.

    // Actually, let's just fetch workflow templates.
    const fetchWorkflows = async () => {
        setLoading(true);
        try {
            const data = await WorkflowApi.listTemplates();
            setWorkflows(data || []);
        } catch (error) {
            toast.error("Failed to load workflows");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchWorkflows();
    }, []);

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            // Hardcoding Product ID for "Memo System" for now as we don't have product selection here yet.
            // Ideally we get this from config or auth.
            const payload = { ...newWorkflow, productId: '3fa85f64-5717-4562-b3fc-2c963f66afa6' }; // Placeholder UUID
            await WorkflowApi.createTemplate(payload);
            toast.success("Workflow created");
            setIsCreateOpen(false);
            setNewWorkflow({ key: '', name: '', description: '', productId: payload.productId });
            fetchWorkflows();
        } catch (error) {
            toast.error("Failed to create workflow");
        }
    };

    const openDesigner = (workflow) => {
        setSelectedWorkflow(workflow);
        setIsDesignerOpen(true);
    };

    const handleSaveDiagram = async (xml) => {
        if (!selectedWorkflow) return;
        try {
            await WorkflowApi.updateTemplate(selectedWorkflow.id, { bpmnXml: xml });
            toast.success("Workflow diagram saved");
            fetchWorkflows(); // Refresh to update timestamp etc
        } catch (error) {
            toast.error("Failed to save diagram");
        }
    };

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="space-y-1">
                    <CardTitle>Workflow Definitions</CardTitle>
                    <CardDescription>Design approval processes for memo topics.</CardDescription>
                </div>
                <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
                    <DialogTrigger asChild>
                        <Button size="sm"><Plus className="mr-2 h-4 w-4" /> New Workflow</Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Create Workflow</DialogTitle>
                            <DialogDescription>Define a new process definition.</DialogDescription>
                        </DialogHeader>
                        <form onSubmit={handleCreate}>
                            <div className="grid gap-4 py-4">
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="w-key" className="text-right">Key</Label>
                                    <Input id="w-key" value={newWorkflow.key} onChange={e => setNewWorkflow({ ...newWorkflow, key: e.target.value })} className="col-span-3" placeholder="e.g. memo_approval" required />
                                </div>
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="w-name" className="text-right">Name</Label>
                                    <Input id="w-name" value={newWorkflow.name} onChange={e => setNewWorkflow({ ...newWorkflow, name: e.target.value })} className="col-span-3" placeholder="Memo Approval" required />
                                </div>
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="w-desc" className="text-right">Description</Label>
                                    <Input id="w-desc" value={newWorkflow.description} onChange={e => setNewWorkflow({ ...newWorkflow, description: e.target.value })} className="col-span-3" />
                                </div>
                            </div>
                            <DialogFooter>
                                <Button type="submit">Create</Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>
                            <TableHead>Key</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {loading ? (
                            <TableRow>
                                <TableCell colSpan={4} className="text-center h-24"><Loader2 className="h-6 w-6 animate-spin mx-auto" /></TableCell>
                            </TableRow>
                        ) : workflows.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={4} className="text-center text-muted-foreground">No workflows found.</TableCell>
                            </TableRow>
                        ) : (
                            workflows.map(wf => (
                                <TableRow key={wf.id}>
                                    <TableCell className="font-medium">
                                        {wf.name}
                                        <div className="text-xs text-muted-foreground">{wf.description}</div>
                                    </TableCell>
                                    <TableCell className="font-mono text-xs">{wf.key}</TableCell>
                                    <TableCell>
                                        <div className={`text-xs px-2 py-1 rounded-full inline-block font-medium ${wf.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                                            {wf.status}
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button variant="outline" size="sm" onClick={() => openDesigner(wf)}>
                                            Design
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </CardContent>

            {/* Full Screen Designer Modal */}
            {selectedWorkflow && isDesignerOpen && (
                <div className="fixed inset-0 z-50 bg-background flex flex-col">
                    <div className="border-b p-4 flex items-center justify-between bg-card">
                        <div>
                            <h2 className="text-lg font-bold flex items-center gap-2">
                                <Button variant="ghost" size="icon" onClick={() => setIsDesignerOpen(false)}>
                                    <ArrowLeft className="h-5 w-5" />
                                </Button>
                                {selectedWorkflow.name}
                                <span className="text-xs font-normal text-muted-foreground ml-2">({selectedWorkflow.key})</span>
                            </h2>
                        </div>
                        <div className="flex gap-2">
                            {selectedWorkflow.status === 'DRAFT' && (
                                <Button variant="default" onClick={() => {
                                    if (confirm('Deploy this workflow?')) {
                                        WorkflowApi.deployTemplate(selectedWorkflow.id)
                                            .then(() => { toast.success("Deployed!"); fetchWorkflows(); setIsDesignerOpen(false); })
                                            .catch(() => toast.error("Failed to deploy"));
                                    }
                                }}>Deploy</Button>
                            )}
                        </div>
                    </div>
                    <div className="flex-1 bg-muted/10 p-4 overflow-hidden">
                        <BpmnDesigner
                            initialXml={selectedWorkflow.bpmnXml}
                            onSave={handleSaveDiagram}
                            readOnly={selectedWorkflow.status !== 'DRAFT'}
                            height="100%"
                        />
                    </div>
                </div>
            )}
        </Card>
    );
}

function CategoriesTab({ categories, onRefresh }) {
    const [isOpen, setIsOpen] = useState(false);
    const [newCategory, setNewCategory] = useState({ code: '', name: '', description: '' });
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await MemoApi.createCategory(newCategory);
            toast.success("Category created successfully");
            setIsOpen(false);
            setNewCategory({ code: '', name: '', description: '' });
            onRefresh();
        } catch (error) {
            toast.error("Failed to create category");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="space-y-1">
                    <CardTitle>Memo Categories</CardTitle>
                    <CardDescription>Define the high-level categories for memos.</CardDescription>
                </div>
                <Dialog open={isOpen} onOpenChange={setIsOpen}>
                    <DialogTrigger asChild>
                        <Button size="sm"><Plus className="mr-2 h-4 w-4" /> Add Category</Button>
                    </DialogTrigger>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>Add New Category</DialogTitle>
                            <DialogDescription>Create a new category for grouping memos.</DialogDescription>
                        </DialogHeader>
                        <form onSubmit={handleSubmit}>
                            <div className="grid gap-4 py-4">
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="code" className="text-right">Code</Label>
                                    <Input id="code" value={newCategory.code} onChange={e => setNewCategory({ ...newCategory, code: e.target.value })} className="col-span-3" required />
                                </div>
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="name" className="text-right">Name</Label>
                                    <Input id="name" value={newCategory.name} onChange={e => setNewCategory({ ...newCategory, name: e.target.value })} className="col-span-3" required />
                                </div>
                                <div className="grid grid-cols-4 items-center gap-4">
                                    <Label htmlFor="desc" className="text-right">Description</Label>
                                    <Input id="desc" value={newCategory.description} onChange={e => setNewCategory({ ...newCategory, description: e.target.value })} className="col-span-3" />
                                </div>
                            </div>
                            <DialogFooter>
                                <Button type="submit" disabled={submitting}>
                                    {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Save
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Code</TableHead>
                            <TableHead>Name</TableHead>
                            <TableHead>Description</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {categories.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={3} className="text-center text-muted-foreground">No categories found.</TableCell>
                            </TableRow>
                        ) : (
                            categories.map(cat => (
                                <TableRow key={cat.id}>
                                    <TableCell className="font-medium">{cat.code}</TableCell>
                                    <TableCell>{cat.name}</TableCell>
                                    <TableCell>{cat.description}</TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    );
}

function TopicsTab({ categories }) {
    const navigate = useNavigate();
    const [selectedCategory, setSelectedCategory] = useState(categories.length > 0 ? categories[0].id : '');
    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(false);

    // Create logic
    const [isOpen, setIsOpen] = useState(false);
    const [newTopic, setNewTopic] = useState({ code: '', name: '', description: '', categoryId: '', numberingPattern: '' });
    const [submitting, setSubmitting] = useState(false);

    // Design modals
    const [designingTopic, setDesigningTopic] = useState(null);
    const [isWorkflowDesignerOpen, setIsWorkflowDesignerOpen] = useState(false);
    const [isFormDesignerOpen, setIsFormDesignerOpen] = useState(false);

    const fetchTopics = async (catId) => {
        if (!catId) return;
        setLoading(true);
        try {
            const data = await MemoApi.getTopics(catId);
            setTopics(data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (selectedCategory) {
            fetchTopics(selectedCategory);
            setNewTopic(prev => ({ ...prev, categoryId: selectedCategory }));
        }
    }, [selectedCategory]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await MemoApi.createTopic(newTopic);
            toast.success("Topic created successfully");
            setIsOpen(false);
            setNewTopic({ code: '', name: '', description: '', categoryId: selectedCategory, numberingPattern: '' });
            fetchTopics(selectedCategory);
        } catch (error) {
            toast.error("Failed to create topic");
        } finally {
            setSubmitting(false);
        }
    };

    const openWorkflowDesigner = (topic) => {
        setDesigningTopic(topic);
        setIsWorkflowDesignerOpen(true);
    };

    const openFormDesigner = (topic) => {
        setDesigningTopic(topic);
        setIsFormDesignerOpen(true);
    };

    const handleSaveWorkflow = async (xml) => {
        if (!designingTopic) return;
        try {
            await MemoApi.updateTopicWorkflow(designingTopic.id, xml);
            toast.success("Workflow saved for topic");
            fetchTopics(selectedCategory);
        } catch (error) {
            toast.error("Failed to save workflow");
        }
    };

    const handleSaveFormSchema = async (schema) => {
        if (!designingTopic) return;
        try {
            await MemoApi.updateTopicFormSchema(designingTopic.id, schema);
            toast.success("Form schema saved for topic");
            fetchTopics(selectedCategory);
            setIsFormDesignerOpen(false);
        } catch (error) {
            toast.error("Failed to save form schema");
        }
    };

    return (
        <>
            <Card>
                <CardHeader className="space-y-4">
                    <div className="flex items-center justify-between">
                        <div className="space-y-1">
                            <CardTitle>Memo Topics</CardTitle>
                            <CardDescription>Topics belong to categories and define specific memo types. Design workflows and forms for each.</CardDescription>
                        </div>
                        <Dialog open={isOpen} onOpenChange={setIsOpen}>
                            <DialogTrigger asChild>
                                <Button size="sm" disabled={!selectedCategory}><Plus className="mr-2 h-4 w-4" /> Add Topic</Button>
                            </DialogTrigger>
                            <DialogContent>
                                <DialogHeader>
                                    <DialogTitle>Add New Topic</DialogTitle>
                                    <DialogDescription>Create a topic under the selected category.</DialogDescription>
                                </DialogHeader>
                                <form onSubmit={handleSubmit}>
                                    <div className="grid gap-4 py-4">
                                        <div className="grid grid-cols-4 items-center gap-4">
                                            <Label htmlFor="t-code" className="text-right">Code</Label>
                                            <Input id="t-code" value={newTopic.code} onChange={e => setNewTopic({ ...newTopic, code: e.target.value })} className="col-span-3" required />
                                        </div>
                                        <div className="grid grid-cols-4 items-center gap-4">
                                            <Label htmlFor="t-name" className="text-right">Name</Label>
                                            <Input id="t-name" value={newTopic.name} onChange={e => setNewTopic({ ...newTopic, name: e.target.value })} className="col-span-3" required />
                                        </div>
                                        <div className="grid grid-cols-4 items-center gap-4">
                                            <Label htmlFor="t-desc" className="text-right">Description</Label>
                                            <Input id="t-desc" value={newTopic.description} onChange={e => setNewTopic({ ...newTopic, description: e.target.value })} className="col-span-3" />
                                        </div>
                                        <div className="grid grid-cols-4 items-center gap-4">
                                            <Label htmlFor="t-pattern" className="text-right">Numbering</Label>
                                            <Input
                                                id="t-pattern"
                                                value={newTopic.numberingPattern}
                                                onChange={e => setNewTopic({ ...newTopic, numberingPattern: e.target.value })}
                                                className="col-span-3"
                                                placeholder="e.g., CAM-%FY%-%SEQ%"
                                                required
                                            />
                                        </div>
                                    </div>
                                    <DialogFooter>
                                        <Button type="submit" disabled={submitting}>
                                            {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Save
                                        </Button>
                                    </DialogFooter>
                                </form>
                            </DialogContent>
                        </Dialog>
                    </div>

                    <div className="flex items-center space-x-2">
                        <Label htmlFor="category-select">Filter by Category:</Label>
                        <select
                            id="category-select"
                            className="flex h-10 w-[200px] items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background"
                            value={selectedCategory}
                            onChange={(e) => setSelectedCategory(e.target.value)}
                        >
                            {categories.map(cat => (
                                <option key={cat.id} value={cat.id}>{cat.name}</option>
                            ))}
                        </select>
                    </div>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Code</TableHead>
                                <TableHead>Name</TableHead>
                                <TableHead>Workflow</TableHead>
                                <TableHead>Form</TableHead>
                                <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="text-center h-24"><Loader2 className="h-6 w-6 animate-spin mx-auto" /></TableCell>
                                </TableRow>
                            ) : topics.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="text-center text-muted-foreground">No topics found for this category.</TableCell>
                                </TableRow>
                            ) : (
                                topics.map(topic => (
                                    <TableRow key={topic.id}>
                                        <TableCell className="font-medium">{topic.code}</TableCell>
                                        <TableCell>{topic.name}</TableCell>
                                        <TableCell>
                                            <span className={`text-xs px-2 py-1 rounded ${topic.workflowXml ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                                                {topic.workflowXml ? 'Configured' : 'Not Set'}
                                            </span>
                                        </TableCell>
                                        <TableCell>
                                            <span className={`text-xs px-2 py-1 rounded ${topic.formSchema ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                                                {topic.formSchema ? 'Configured' : 'Not Set'}
                                            </span>
                                        </TableCell>
                                        <TableCell className="text-right space-x-2">
                                            <Button
                                                variant="default"
                                                size="sm"
                                                className="bg-blue-600 hover:bg-blue-700"
                                                onClick={() => navigate(`/workflow/${topic.id}`)}
                                            >
                                                <Workflow className="w-3 h-3 mr-1" />
                                                Design Workflow
                                            </Button>
                                            <Button variant="outline" size="sm" onClick={() => openFormDesigner(topic)}>
                                                Form
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>

            {/* Full Screen Workflow Designer Modal */}
            {designingTopic && isWorkflowDesignerOpen && (
                <div className="fixed inset-0 z-50 bg-background flex flex-col">
                    <div className="border-b p-4 flex items-center justify-between bg-card">
                        <h2 className="text-lg font-bold flex items-center gap-2">
                            <Button variant="ghost" size="icon" onClick={() => setIsWorkflowDesignerOpen(false)}>
                                <ArrowLeft className="h-5 w-5" />
                            </Button>
                            Workflow for: {designingTopic.name}
                        </h2>
                    </div>
                    <div className="flex-1 bg-muted/10 p-4 overflow-hidden">
                        <BpmnDesigner
                            initialXml={designingTopic.workflowXml}
                            onSave={handleSaveWorkflow}
                            height="100%"
                        />
                    </div>
                </div>
            )}

            {/* Form Schema Designer Modal */}
            {designingTopic && isFormDesignerOpen && (
                <FormSchemaDesigner
                    topic={designingTopic}
                    onSave={handleSaveFormSchema}
                    onClose={() => setIsFormDesignerOpen(false)}
                />
            )}
        </>
    );
}

// Simple Form Schema Designer Component
function FormSchemaDesigner({ topic, onSave, onClose }) {
    const [schemaText, setSchemaText] = useState(
        topic.formSchema ? JSON.stringify(topic.formSchema, null, 2) : `{
  "type": "object",
  "properties": {
    "amount": {
      "type": "number",
      "title": "Amount"
    },
    "justification": {
      "type": "string",
      "title": "Justification"
    }
  },
  "required": ["amount"]
}`
    );
    const [error, setError] = useState(null);

    const handleSave = () => {
        try {
            const parsed = JSON.parse(schemaText);
            setError(null);
            onSave(parsed);
        } catch (e) {
            setError("Invalid JSON: " + e.message);
        }
    };

    return (
        <div className="fixed inset-0 z-50 bg-background flex flex-col">
            <div className="border-b p-4 flex items-center justify-between bg-card">
                <h2 className="text-lg font-bold flex items-center gap-2">
                    <Button variant="ghost" size="icon" onClick={onClose}>
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    Form Schema for: {topic.name}
                </h2>
                <Button onClick={handleSave}>Save Schema</Button>
            </div>
            <div className="flex-1 p-4 overflow-auto">
                {error && <div className="mb-4 p-3 bg-red-100 text-red-700 rounded">{error}</div>}
                <p className="text-sm text-muted-foreground mb-2">Define JSON Schema for dynamic form fields. This follows the JSON Schema standard.</p>
                <textarea
                    className="w-full h-[calc(100vh-200px)] font-mono text-sm p-4 border rounded-lg bg-muted/20"
                    value={schemaText}
                    onChange={(e) => setSchemaText(e.target.value)}
                />
            </div>
        </div>
    );
}
