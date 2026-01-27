import React from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { BarChart3, TrendingUp, Users, Clock, AlertTriangle } from 'lucide-react';
import { PageContainer } from '../components/PageContainer';

export default function ReportsPage() {
    return (
        <PageContainer>
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-brand-navy">Executive Reports</h1>
                    <p className="text-slate-500 text-sm">Organizational performance and workflow analytics (Mock Data)</p>
                </div>
                <div className="flex items-center gap-2">
                    <button className="text-sm font-medium text-brand-blue bg-blue-50 px-3 py-1.5 rounded-md hover:bg-blue-100 transition-colors">
                        Export to PDF
                    </button>
                    <button className="text-sm font-medium text-brand-blue bg-blue-50 px-3 py-1.5 rounded-md hover:bg-blue-100 transition-colors">
                        Export CSV
                    </button>
                </div>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                {/* Department Throughput */}
                <Card className="border shadow-sm">
                    <CardHeader>
                        <CardTitle className="text-lg flex items-center gap-2">
                            <BarChart3 className="h-5 w-5 text-brand-blue" />
                            Department Throughput
                        </CardTitle>
                        <CardDescription>Memos processed vs rejected by department</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            {['IT Infrastructure', 'Human Resources', 'Finance & Procurement', 'Legal Compliance', 'Marketing'].map(dept => (
                                <div key={dept} className="space-y-1">
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="font-medium text-slate-700">{dept}</span>
                                        <span className="text-slate-500">85% Approval Rate</span>
                                    </div>
                                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden flex">
                                        <div className="h-full bg-brand-navy w-[65%]" title="Approved"></div>
                                        <div className="h-full bg-red-400 w-[10%]" title="Rejected"></div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                {/* SLA Breaches */}
                <Card className="border shadow-sm">
                    <CardHeader>
                        <CardTitle className="text-lg flex items-center gap-2">
                            <Clock className="h-5 w-5 text-red-500" />
                            SLA Compliance
                        </CardTitle>
                        <CardDescription>Average processing time vs target (48 hours)</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-6">
                            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-100">
                                <div className="flex items-center gap-3">
                                    <div className="bg-red-100 p-2 rounded-full text-red-600"><AlertTriangle className="h-5 w-5" /></div>
                                    <div>
                                        <div className="font-bold text-slate-900">Procurement Requests</div>
                                        <div className="text-xs text-slate-500">Target: 2 days</div>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <div className="text-2xl font-bold text-red-600">4.5 Days</div>
                                    <div className="text-xs text-red-500 font-medium">+125% over target</div>
                                </div>
                            </div>

                            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-100">
                                <div className="flex items-center gap-3">
                                    <div className="bg-green-100 p-2 rounded-full text-green-600"><CheckCircleIcon className="h-5 w-5" /></div>
                                    <div>
                                        <div className="font-bold text-slate-900">Travel Expenses</div>
                                        <div className="text-xs text-slate-500">Target: 3 days</div>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <div className="text-2xl font-bold text-green-600">1.2 Days</div>
                                    <div className="text-xs text-green-500 font-medium">Within SLA</div>
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>

            <Card className="border shadow-sm">
                <CardHeader>
                    <CardTitle>System Usage Trends</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="h-64 flex items-center justify-center bg-slate-50 text-slate-400 border-2 border-dashed rounded-lg">
                        Dynamic Chart Visualization Placeholder
                    </div>
                </CardContent>
            </Card>
        </PageContainer>
    );
}

const CheckCircleIcon = ({ className }) => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
        <polyline points="22 4 12 14.01 9 11.01" />
    </svg>
);
