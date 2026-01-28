import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MemoApi } from '../lib/api';
import { PageContainer } from '../components/PageContainer';
import { Button } from '../components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Loader2, FileText, Calendar, ArrowRight, Edit, FolderOpen, PlusCircle, Sparkles } from 'lucide-react';
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
            const memos = await MemoApi.getMyMemos();
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
        <PageContainer className="p-0">
            {/* Premium Dark Header */}
            <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 px-8 py-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                        <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center shadow-lg shadow-amber-500/20">
                            <FolderOpen className="h-6 w-6 text-white" />
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-white">My Drafts</h1>
                            <p className="text-slate-400 text-sm">Manage your unfinished memos and continue editing</p>
                        </div>
                    </div>
                    <Button
                        onClick={() => navigate('/create')}
                        className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white shadow-lg shadow-blue-500/25 rounded-xl"
                    >
                        <PlusCircle className="mr-2 h-4 w-4" />
                        New Memo
                    </Button>
                </div>
            </div>

            {/* Table Card */}
            <div className="p-6 md:p-8">
                <div className="rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-slate-50/80 border-b border-slate-100">
                                <TableHead className="w-[40%] font-semibold text-slate-600">Subject</TableHead>
                                <TableHead className="font-semibold text-slate-600">Category</TableHead>
                                <TableHead className="font-semibold text-slate-600">Last Updated</TableHead>
                                <TableHead className="font-semibold text-slate-600">Priority</TableHead>
                                <TableHead className="text-right font-semibold text-slate-600">Action</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="h-40 text-center">
                                        <div className="flex flex-col items-center justify-center gap-3">
                                            <div className="animate-spin rounded-full h-10 w-10 border-3 border-blue-200 border-t-blue-600"></div>
                                            <span className="text-slate-500 font-medium">Loading drafts...</span>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : drafts.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={5} className="h-64 text-center">
                                        <div className="flex flex-col items-center justify-center gap-4">
                                            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-50 flex items-center justify-center shadow-inner">
                                                <Sparkles className="h-8 w-8 text-slate-300" />
                                            </div>
                                            <div>
                                                <p className="font-semibold text-slate-700">No drafts found</p>
                                                <p className="text-slate-400 text-sm mt-1">Start creating a new memo</p>
                                            </div>
                                            <Button
                                                onClick={() => navigate('/create')}
                                                className="mt-2 bg-blue-50 text-blue-600 hover:bg-blue-100"
                                            >
                                                <PlusCircle className="mr-2 h-4 w-4" />
                                                Create Memo
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                drafts.map((draft) => (
                                    <TableRow
                                        key={draft.id}
                                        className="group cursor-pointer hover:bg-blue-50/50 transition-all duration-200 border-b border-slate-100 last:border-0"
                                        onClick={() => handleContinue(draft.id)}
                                    >
                                        <TableCell>
                                            <div className="font-semibold text-slate-900 group-hover:text-blue-700 transition-colors">
                                                {draft.subject || <i className="text-slate-400">Untitled Draft</i>}
                                            </div>
                                            <div className="text-xs text-slate-400 font-mono mt-1">{draft.memoNumber}</div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex flex-col">
                                                <span className="text-sm text-slate-700">{draft.categoryName || '-'}</span>
                                                <span className="text-xs text-slate-400">{draft.topicName}</span>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center text-slate-500 text-sm">
                                                <Calendar className="h-3.5 w-3.5 mr-1.5 text-slate-400" />
                                                {new Date(draft.updatedAt || draft.createdAt).toLocaleDateString()}
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <span className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-semibold border
                                                ${draft.priority === 'HIGH' ? 'bg-amber-50 text-amber-600 border-amber-200' :
                                                    draft.priority === 'URGENT' ? 'bg-red-50 text-red-600 border-red-200' :
                                                        'bg-slate-50 text-slate-600 border-slate-200'}`}>
                                                {draft.priority}
                                            </span>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                size="sm"
                                                className="bg-blue-50 text-blue-600 hover:bg-blue-100 border-0 shadow-none group-hover:bg-blue-500 group-hover:text-white transition-all"
                                                onClick={(e) => { e.stopPropagation(); handleContinue(draft.id); }}
                                            >
                                                Continue <ArrowRight className="ml-1.5 h-3.5 w-3.5" />
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </div>
            </div>
        </PageContainer>
    );
}

