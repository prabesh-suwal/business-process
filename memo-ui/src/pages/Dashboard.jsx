import React, { useEffect, useState } from 'react';
import { MemoApi } from '../lib/api';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Plus, Calendar, ArrowRight } from 'lucide-react';
import { format } from 'date-fns';

export default function Dashboard() {
    const [memos, setMemos] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        MemoApi.getMyMemos()
            .then(setMemos)
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <div className="p-8 flex items-center justify-center h-full text-muted-foreground">Loading dashboard...</div>;
    }

    return (
        <div className="space-y-6 container mx-auto p-6 max-w-7xl animate-in fade-in duration-500">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
                    <p className="text-muted-foreground">Manage your memos and drafts.</p>
                </div>
                <Link to="/create">
                    <Button className="shadow-lg hover:shadow-xl transition-all">
                        <Plus className="mr-2 h-4 w-4" /> New Memo
                    </Button>
                </Link>
            </div>

            <Card className="border-t-4 border-t-primary shadow-md">
                <CardHeader>
                    <CardTitle>My Memos</CardTitle>
                    <CardDescription>A list of all memos you have created or submitted.</CardDescription>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-[120px]">Memo #</TableHead>
                                <TableHead>Subject</TableHead>
                                <TableHead>Topic</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>Created</TableHead>
                                <TableHead className="text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {memos.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                                        No memos found. Create one to get started.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                memos.map((memo) => (
                                    <TableRow key={memo.id} className="cursor-pointer hover:bg-muted/50">
                                        <TableCell className="font-mono font-medium text-primary">
                                            {memo.memoNumber}
                                        </TableCell>
                                        <TableCell>
                                            <div className="font-medium text-base">{memo.subject}</div>
                                            <div className="text-xs text-muted-foreground truncate max-w-[300px]">
                                                {memo.categoryName}
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant="outline" className="font-normal border-gray-300">
                                                {memo.topicName}
                                            </Badge>
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant={
                                                memo.status === 'DRAFT' ? 'secondary' :
                                                    memo.status === 'APPROVED' ? 'success' :
                                                        memo.status === 'URGENT' ? 'destructive' : 'default'
                                            }>
                                                {memo.status}
                                            </Badge>
                                        </TableCell>
                                        <TableCell className="text-muted-foreground">
                                            <div className="flex items-center gap-2 text-sm">
                                                <Calendar className="h-3 w-3" />
                                                {format(new Date(memo.createdAt), 'MMM d, yyyy')}
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Link to={`/edit/${memo.id}`}>
                                                <Button variant="ghost" size="sm">
                                                    Open <ArrowRight className="ml-2 h-3 w-3" />
                                                </Button>
                                            </Link>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>
        </div>
    );
}
