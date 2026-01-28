import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Activity, Clock, CheckCircle, FileText, TrendingUp, Users, ArrowUpRight, ArrowDownRight, AlertCircle, Calendar, LayoutDashboard } from 'lucide-react';
import { MemoApi } from '../lib/api';
import { Link } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { PageContainer } from '../components/PageContainer';

const StatCard = ({ title, value, change, trend, subtext, icon: Icon, badge }) => (
    <Card className="border border-slate-200 shadow-sm bg-white hover:shadow-md transition-all duration-200 rounded-xl">
        <CardContent className="p-6">
            <div className="flex items-start justify-between">
                <div>
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">{title}</p>
                    <div className="flex items-baseline gap-2 mt-2">
                        <div className="text-3xl font-bold text-slate-900 leading-none">{value}</div>
                        {change && (
                            <span className={`text-xs font-bold ${trend === 'up' ? 'text-emerald-600' : trend === 'down' ? 'text-red-500' : 'text-slate-500'}`}>
                                {trend === 'up' ? '↑' : '↓'} {change}
                            </span>
                        )}
                    </div>
                    {subtext && <p className="text-xs text-slate-400 mt-2">{subtext}</p>}
                </div>
                <div className={`p-2 rounded-xl ${badge ? 'bg-transparent' : 'bg-slate-100'}`}>
                    {badge ? (
                        badge
                    ) : (
                        <Icon className="h-5 w-5 text-blue-600" />
                    )}
                </div>
            </div>
        </CardContent>
    </Card>
);

export default function Dashboard() {
    const [stats, setStats] = useState({
        pending: 24,
        drafts: 5,
        approved: 1284,
        rejected: 3
    });

    return (
        <PageContainer className="p-0">
            {/* Premium Dark Header */}
            <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 px-8 py-6">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                            <LayoutDashboard className="h-6 w-6 text-white" />
                        </div>
                        <div>
                            <p className="text-xs font-bold text-blue-400 uppercase tracking-wider">Overview</p>
                            <h1 className="text-2xl font-bold text-white">Executive Dashboard</h1>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <Button variant="ghost" className="text-slate-300 hover:text-white hover:bg-white/10 rounded-lg">
                            <Calendar className="mr-2 h-4 w-4" />
                            Last 30 Days
                        </Button>
                        <Button variant="ghost" className="text-slate-300 hover:text-white hover:bg-white/10 rounded-lg">
                            <FileText className="mr-2 h-4 w-4" />
                            Export
                        </Button>
                    </div>
                </div>
            </div>

            <div className="p-6 md:p-8 space-y-6">
                <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
                    <StatCard
                        title="Total Active Memos"
                        value={stats.approved.toLocaleString()}
                        change="12.5%"
                        trend="up"
                        subtext="Across 8 departments"
                        icon={FileText}
                    />
                    <StatCard
                        title="Avg. Approval Time"
                        value="4.2"
                        subtext="Goal: 3.5 Days"
                        change="0.5%"
                        trend="down"
                        icon={Clock}
                        badge={<div className="text-xs font-bold text-2xl text-slate-900">Days</div>} // Custom layout tweak
                    />
                    <StatCard
                        title="Pending Your Action"
                        value={stats.pending}
                        subtext="Estimated review: 2 hrs"
                        badge={<span className="bg-red-100 text-red-600 text-[10px] uppercase font-bold px-1.5 py-0.5 rounded">Urgent</span>}
                        icon={AlertCircle}
                    />
                    <StatCard
                        title="Efficiency Rating"
                        value="94.2%"
                        change="2.1%"
                        trend="up"
                        subtext="Target benchmark met"
                        icon={CheckCircle}
                        badge={<CheckCircle className="h-5 w-5 text-green-500 fill-green-100" />}
                    />
                </div>

                {/* Main Dashboard Content */}
                <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-2">
                    {/* Chart 1: Memo Status (Donut) */}
                    <Card className="border-none shadow-sm h-full">
                        <CardHeader className="flex flex-row items-center justify-between pb-2">
                            <CardTitle className="text-lg font-bold text-slate-900">Memo Status</CardTitle>
                            <Button variant="ghost" size="icon" className="h-8 w-8"><div className="flex gap-0.5"><div className="h-1 w-1 bg-slate-400 rounded-full"></div><div className="h-1 w-1 bg-slate-400 rounded-full"></div><div className="h-1 w-1 bg-slate-400 rounded-full"></div></div></Button>
                        </CardHeader>
                        <CardContent className="flex items-center justify-center p-6 relative">
                            {/* CSS Donut Chart */}
                            <div className="relative h-64 w-64">
                                <svg viewBox="0 0 100 100" className="transform -rotate-90 w-full h-full">
                                    <circle cx="50" cy="50" r="40" fill="transparent" stroke="#EFF6FF" strokeWidth="10" />
                                    <circle cx="50" cy="50" r="40" fill="transparent" stroke="#2563EB" strokeWidth="10" strokeDasharray="200 251.2" strokeLinecap="round" />
                                </svg>
                                <div className="absolute inset-0 flex flex-col items-center justify-center">
                                    <span className="text-3xl font-bold text-slate-900">{stats.approved.toLocaleString()}</span>
                                    <span className="text-xs text-slate-500 uppercase font-bold tracking-wider">Total</span>
                                </div>
                            </div>
                            <div className="absolute bottom-4 right-8 text-xs space-y-2">
                                <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-brand-blue"></div><span className="font-semibold text-slate-700">Approved 65%</span></div>
                                <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-amber-400"></div><span className="font-semibold text-slate-700">Pending 28%</span></div>
                                <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-red-400"></div><span className="font-semibold text-slate-700">Overdue 7%</span></div>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Chart 2: Turnaround Time Trend (Bar) */}
                    <Card className="border-none shadow-sm h-full">
                        <CardHeader className="flex flex-row items-center justify-between pb-2">
                            <CardTitle className="text-lg font-bold text-slate-900">Turnaround Time Trend (Days)</CardTitle>
                            <div className="flex gap-4 text-[10px] font-bold text-slate-400 uppercase">
                                <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-brand-blue"></div> This Month</div>
                                <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-slate-200"></div> Prev. Month</div>
                            </div>
                        </CardHeader>
                        <CardContent className="h-[300px] flex items-end justify-between gap-2 px-6 pt-10 pb-4">
                            {[40, 60, 45, 75, 90, 60, 60, 40, 70, 100, 80].map((h, i) => (
                                <div key={i} className="w-full flex flex-col justify-end h-full gap-1 group">
                                    <div style={{ height: `${h}%` }} className={`w-full rounded-t-sm transition-all duration-300 ${i === 4 || i === 9 ? 'bg-brand-blue' : 'bg-brand-blue/20 hover:bg-brand-blue/40'}`}></div>
                                </div>
                            ))}
                        </CardContent>
                        <div className="flex justify-between px-6 pb-6 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                            <span>Week 1</span>
                            <span>Week 2</span>
                            <span>Week 3</span>
                            <span>Week 4</span>
                        </div>
                    </Card>
                </div>

                {/* Bottom Section: Awaiting Review */}
                <Card className="border-none shadow-sm">
                    <CardHeader className="flex flex-row items-center justify-between pb-0">
                        <CardTitle className="text-lg font-bold text-slate-900">Memos Awaiting Review</CardTitle>
                        <Button variant="ghost" size="sm" className="bg-slate-50 hover:bg-slate-100 text-slate-600 text-xs font-bold">Go to Inbox</Button>
                    </CardHeader>
                    <CardContent className="pt-6">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm text-left">
                                <thead className="bg-white border-b border-slate-100 text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                                    <tr>
                                        <th className="py-3 pl-4">Memo ID</th>
                                        <th className="py-3">Title</th>
                                        <th className="py-3">Dept</th>
                                        <th className="py-3">Priority</th>
                                        <th className="py-3 text-right pr-4">Action</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50">
                                    {[
                                        { id: '#MEM-9021', title: 'Q3 Credit Risk Audit', dept: 'Risk', priority: 'HIGH', author: 'Sarah Jenkins' },
                                        { id: '#MEM-9022', title: 'IT Infrastructure Upgrade', dept: 'IT Ops', priority: 'NORMAL', author: 'Mike Ross' },
                                        { id: '#MEM-9023', title: 'Vendor Contract Renewal', dept: 'Procurement', priority: 'URGENT', author: 'Harvey Specter' },
                                    ].map((item) => (
                                        <tr key={item.id} className="group hover:bg-slate-50/50 transition-colors">
                                            <td className="py-4 pl-4 font-mono text-xs font-bold text-brand-navy">{item.id}</td>
                                            <td className="py-4">
                                                <div className="font-bold text-slate-900">{item.title}</div>
                                                <div className="text-xs text-slate-500">by {item.author} • 2h ago</div>
                                            </td>
                                            <td className="py-4 text-slate-600">{item.dept}</td>
                                            <td className="py-4">
                                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${item.priority === 'URGENT' ? 'bg-red-100 text-red-600' :
                                                    item.priority === 'HIGH' ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-600'
                                                    }`}>
                                                    {item.priority}
                                                </span>
                                            </td>
                                            <td className="py-4 text-right pr-4">
                                                <Button variant="link" className="text-brand-blue font-bold hover:text-blue-700 p-0 h-auto">Review</Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </PageContainer>
    );
}
