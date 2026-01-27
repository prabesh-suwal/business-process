import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { PageContainer } from '../components/PageContainer';
import { Button } from '../components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Loader2, FileText, Calendar, ArrowRight, Edit } from 'lucide-react';
import { toast } from 'sonner';

export default function Drafts() {
    const navigate = useNavigate();
    const [drafts, setDrafts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadDrafts();
    }, []);

    const loadDrafts = async () => {
        setLoading(true);
        try {
            // Assuming getMyMemos returns all memos created by user
            const memos = await MemoApi.getMyMemos();
            // Filter locally for drafts if API returns everything. 
            // Ideally API should filter, but for now we filter here.
            const draftMemos = memos.filter(m => m.status === 'DRAFT' || m.status === 'SENT_BACK');
            setDrafts(draftMemos);
        } catch (error) {
            console.error(error);
            toast.error("Failed to load drafts");
        } finally {
            setLoading(false);
        }
    };

    const handleContinue = (id) => {
        navigate(`/memo/${id}`);
    };

    return (
        <PageContainer>
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">My Drafts</h1>
                    <p className="text-slate-500 text-sm mt-1">Manage your unfinished memos</p>
                </div>
                <Button onClick={() => navigate('/create')}>
                    Create New Memo
                </Button>
            </div>

            <div className="rounded-lg border border-slate-200 bg-white shadow-sm overflow-hidden min-h-[400px]">
                <Table>
                    <TableHeader className="bg-slate-50">
                        <TableRow>
                            <TableHead className="w-[40%]">Subject</TableHead>
                            <TableHead>Category</TableHead>
                            <TableHead>Last Updated</TableHead>
                            <TableHead>Priority</TableHead>
                            <TableHead className="text-right">Action</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {loading ? (
                            <TableRow>
                                <TableCell colSpan={5} className="h-40 text-center">
                                    <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
                                        <Loader2 className="h-6 w-6 animate-spin" />
                                        <span>Loading drafts...</span>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : drafts.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} className="h-64 text-center">
                                    <div className="flex flex-col items-center justify-center text-slate-400 gap-3">
                                        <div className="h-12 w-12 rounded-full bg-slate-100 flex items-center justify-center">
                                            <FileText className="h-6 w-6 text-slate-300" />
                                        </div>
                                        <p>No drafts found.</p>
                                        <Button variant="link" onClick={() => navigate('/create')}>Start a new memo</Button>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : (
                            drafts.map((draft) => (
                                <TableRow key={draft.id} className="cursor-pointer hover:bg-slate-50/50" onClick={() => handleContinue(draft.id)}>
                                    <TableCell>
                                        <div className="font-medium text-slate-900">{draft.subject || <i>Untitled Draft</i>}</div>
                                        <div className="text-xs text-slate-500 font-mono mt-1">{draft.memoNumber}</div>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex flex-col">
                                            <span className="text-sm">{draft.categoryName || '-'}</span>
                                            <span className="text-xs text-slate-400">{draft.topicName}</span>
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center text-slate-500 text-xs">
                                            <Calendar className="h-3.5 w-3.5 mr-1.5" />
                                            {new Date(draft.updatedAt || draft.createdAt).toLocaleDateString()}
                                        </div>
                                    </TableCell>
                                    <TableCell>
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium 
                                            ${draft.priority === 'HIGH' ? 'bg-orange-100 text-orange-800' :
                                                draft.priority === 'URGENT' ? 'bg-red-100 text-red-800' :
                                                    'bg-slate-100 text-slate-800'}`}>
                                            {draft.priority}
                                        </span>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button size="sm" variant="ghost" className="text-brand-blue hover:text-brand-blue hover:bg-blue-50" onClick={(e) => { e.stopPropagation(); handleContinue(draft.id); }}>
                                            Edit <Edit className="ml-1.5 h-3.5 w-3.5" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>
        </PageContainer>
    );
}
