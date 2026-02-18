import React from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
    LayoutDashboard,
    PlusCircle,
    FileText,
    Settings,
    LogOut,
    Search,
    Bell,
    Menu,
    ChevronDown,
    User,
    Eye,
    BarChart3,
    Archive,
    Users,
    Inbox,
    FolderOpen,
    GitBranch,
    Sparkles,
    Table2
} from 'lucide-react';
import { cn } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { Button } from './ui/button';
import { Toaster } from 'sonner';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "./ui/dropdown-menu";

const SidebarItem = ({ to, icon: Icon, label, notifications, badge }) => {
    const location = useLocation();
    const isActive = location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

    return (
        <Link
            to={to}
            className={cn(
                "flex items-center justify-between px-3 py-2.5 mx-2 rounded-xl transition-all duration-200 group text-sm font-medium",
                isActive
                    ? "bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-lg shadow-blue-500/25"
                    : "text-slate-400 hover:bg-white/5 hover:text-white"
            )}
        >
            <div className="flex items-center gap-3">
                <div className={cn(
                    "p-1.5 rounded-lg transition-colors",
                    isActive
                        ? "bg-white/20"
                        : "bg-slate-800 group-hover:bg-slate-700"
                )}>
                    <Icon className={cn("h-4 w-4", isActive ? "text-white" : "text-slate-400 group-hover:text-white")} />
                </div>
                <span>{label}</span>
            </div>
            {notifications > 0 && (
                <span className={cn(
                    "text-xs px-2 py-0.5 rounded-full font-bold",
                    isActive ? "bg-white/20 text-white" : "bg-blue-500/20 text-blue-400"
                )}>
                    {notifications}
                </span>
            )}
            {badge && (
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold uppercase">
                    {badge}
                </span>
            )}
        </Link>
    );
};

export default function MainLayout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-slate-100 flex text-slate-800 font-sans">
            {/* Premium Dark Sidebar */}
            <aside className="hidden md:flex flex-col w-64 bg-gradient-to-b from-slate-900 via-slate-900 to-slate-950 fixed inset-y-0 z-50">
                {/* Brand Header */}
                <div className="h-16 flex items-center px-5 border-b border-white/5">
                    <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
                            <GitBranch className="h-5 w-5 text-white" />
                        </div>
                        <div>
                            <h1 className="font-bold text-white text-base tracking-tight leading-none">Memo Systems</h1>
                            <p className="text-[10px] text-slate-500 font-medium uppercase tracking-wider mt-0.5">Enterprise Bank</p>
                        </div>
                    </div>
                </div>

                {/* Navigation */}
                <div className="flex-1 py-6 space-y-1 overflow-y-auto">
                    <div className="px-5 pb-3 text-[10px] font-bold text-slate-600 uppercase tracking-widest">Main Menu</div>
                    <SidebarItem to="/" icon={LayoutDashboard} label="Dashboard" />
                    <SidebarItem to="/tasks" icon={Inbox} label="Task Inbox" notifications={3} />
                    <SidebarItem to="/memos" icon={FileText} label="Memos" />
                    <SidebarItem to="/drafts" icon={FolderOpen} label="My Drafts" />
                    <SidebarItem to="/archives" icon={Archive} label="Archives" />

                    <div className="my-4 mx-5 border-t border-white/5"></div>

                    <div className="px-5 pb-3 text-[10px] font-bold text-slate-600 uppercase tracking-widest">Analytics</div>
                    <SidebarItem to="/reports" icon={BarChart3} label="Reports" badge="New" />
                    <SidebarItem to="/dmn" icon={Table2} label="Decision Tables" />
                </div>

                {/* User Profile (Bottom) */}
                <div className="p-4 border-t border-white/5 bg-slate-950/50">
                    <div className="flex items-center gap-3 mb-3 px-1">
                        <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-slate-700 to-slate-800 overflow-hidden border border-white/10 flex items-center justify-center shadow-inner">
                            <span className="text-white font-bold text-sm">
                                {(user?.username && user.username[0] ? user.username[0].toUpperCase() : 'U')}
                            </span>
                        </div>
                        <div className="overflow-hidden flex-1">
                            <p className="text-sm font-semibold text-white truncate">{user?.username || 'User'}</p>
                            <p className="text-xs text-slate-500 truncate">{user?.email || 'Executive Access'}</p>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        size="sm"
                        className="w-full justify-start text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors rounded-lg"
                        onClick={logout}
                    >
                        <LogOut className="mr-2 h-4 w-4" />
                        Sign Out
                    </Button>
                </div>
            </aside>

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col md:ml-64 min-w-0 transition-all duration-300 ease-in-out">
                {/* Premium Global Header */}
                <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-40 flex items-center justify-between px-6 shadow-sm">
                    {/* Search Bar */}
                    <div className="flex-1 max-w-xl relative hidden md:block group">
                        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                            <Search className="h-4 w-4 text-slate-400 group-focus-within:text-blue-500 transition-colors" />
                        </div>
                        <input
                            type="text"
                            placeholder="Search memos, departments, or staff..."
                            className="block w-full pl-11 pr-4 py-2.5 border border-slate-200 bg-slate-50 rounded-xl text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 focus:bg-white transition-all duration-200"
                        />
                    </div>

                    {/* Right Actions */}
                    <div className="flex items-center gap-2 ml-4">
                        <Button size="icon" variant="ghost" className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 relative rounded-xl h-10 w-10">
                            <Bell className="h-5 w-5" />
                            <span className="absolute top-2 right-2 h-2.5 w-2.5 bg-red-500 rounded-full border-2 border-white"></span>
                        </Button>
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost" className="text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl h-10 w-10">
                                    <Settings className="h-5 w-5" />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-56">
                                <DropdownMenuLabel>System Settings</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem onClick={() => navigate('/settings')}>
                                    <Settings className="mr-2 h-4 w-4" />
                                    <span>Memo Configuration</span>
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>

                        <div className="h-8 w-px bg-slate-200 mx-2"></div>

                        <Link to="/create">
                            <Button className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white font-medium shadow-lg shadow-blue-500/25 rounded-xl px-4 h-10">
                                <PlusCircle className="mr-2 h-4 w-4" />
                                New Memo
                            </Button>
                        </Link>
                    </div>
                </header>

                {/* Page Content */}
                <main className="flex-1 overflow-x-hidden">
                    <Outlet />
                </main>
            </div>
            <Toaster />
        </div>
    );
}

