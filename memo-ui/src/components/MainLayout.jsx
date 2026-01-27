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
    Users
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

const SidebarItem = ({ to, icon: Icon, label, notifications }) => {
    const location = useLocation();
    const isActive = location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

    return (
        <Link
            to={to}
            className={cn(
                "flex items-center justify-between px-4 py-3 mx-2 rounded-md transition-all duration-200 group text-sm font-medium",
                isActive
                    ? "bg-brand-blue text-white shadow-md shadow-blue-200"
                    : "text-slate-500 hover:bg-blue-50 hover:text-brand-blue"
            )}
        >
            <div className="flex items-center gap-3">
                <Icon className={cn("h-5 w-5", isActive ? "text-white" : "text-slate-400 group-hover:text-brand-blue")} />
                <span>{label}</span>
            </div>
            {notifications > 0 && (
                <span className={cn(
                    "text-xs px-2 py-0.5 rounded-full font-bold",
                    isActive ? "bg-white/20 text-white" : "bg-brand-blue text-white"
                )}>
                    {notifications}
                </span>
            )}
        </Link>
    );
};

export default function MainLayout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-brand-slate-light flex text-slate-800 font-sans">
            {/* Sidebar (Brand Navy) */}
            <aside className="hidden md:flex flex-col w-64 bg-white border-r border-slate-200 fixed inset-y-0 z-50 shadow-sm">
                {/* Brand Header */}
                <div className="h-16 flex items-center px-6 border-b border-slate-100">
                    <div className="flex items-center gap-3">
                        <div className="bg-brand-blue p-1.5 rounded-lg">
                            <FileText className="h-5 w-5 text-white" />
                        </div>
                        <div>
                            <h1 className="font-bold text-slate-900 text-base tracking-tight leading-none">Memo Systems</h1>
                            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mt-0.5">Enterprise Bank</p>
                        </div>
                    </div>
                </div>

                {/* Navigation */}
                <div className="flex-1 py-6 space-y-1 overflow-y-auto px-3">
                    <div className="px-3 pb-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Main Menu</div>
                    <SidebarItem to="/" icon={LayoutDashboard} label="Dashboard" />
                    <SidebarItem to="/tasks" icon={FileText} label="Inbox" notifications={3} />
                    <SidebarItem to="/drafts" icon={FileText} label="Drafts" />
                    <SidebarItem to="/archives" icon={Archive} label="Archive" />

                    <div className="my-4 border-t border-slate-100 mx-3"></div>

                    <div className="px-3 pb-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">Reporting</div>
                    <SidebarItem to="/reports" icon={BarChart3} label="Performance" />
                    <SidebarItem to="/departments" icon={Users} label="Departments" />
                </div>

                {/* User Profile (Bottom) */}
                <div className="p-4 border-t border-slate-100 bg-slate-50/50">
                    <div className="flex items-center gap-3 mb-3 px-2">
                        <div className="h-10 w-10 rounded-full bg-slate-200 overflow-hidden border-2 border-white shadow-sm">
                            {/* Placeholder for user image if available, else initials */}
                            <div className="h-full w-full flex items-center justify-center text-slate-500 font-bold bg-slate-100">
                                {(user?.username && user.username[0] ? user.username[0].toUpperCase() : 'U')}
                            </div>
                        </div>
                        <div className="overflow-hidden">
                            <p className="text-sm font-bold text-slate-900 truncate">{user?.username || 'User'}</p>
                            <p className="text-xs text-slate-500 truncate">{user?.email || 'Executive Access'}</p>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        size="sm"
                        className="w-full justify-start text-slate-500 hover:text-red-600 hover:bg-red-50 transition-colors"
                        onClick={logout}
                    >
                        <LogOut className="mr-2 h-4 w-4" />
                        Sign Out
                    </Button>
                </div>
            </aside>

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col md:ml-64 min-w-0 transition-all duration-300 ease-in-out">
                {/* Global Header */}
                <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-40 flex items-center justify-between px-6">
                    {/* Search Bar */}
                    <div className="flex-1 max-w-xl relative hidden md:block group">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                            <Search className="h-4 w-4 text-slate-400 group-focus-within:text-brand-blue transition-colors" />
                        </div>
                        <input
                            type="text"
                            placeholder="Search memos, departments, or staff..."
                            className="block w-full pl-10 pr-3 py-2 border-none bg-slate-100 rounded-md text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-blue/20 focus:bg-white transition-all duration-200"
                        />
                    </div>

                    {/* Right Actions */}
                    <div className="flex items-center gap-3 ml-4">
                        <Button size="icon" variant="ghost" className="text-slate-400 hover:text-slate-600 hover:bg-slate-50 relative">
                            <Bell className="h-5 w-5" />
                            <span className="absolute top-2.5 right-2.5 h-2 w-2 bg-red-500 rounded-full border border-white"></span>
                        </Button>
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost" className="text-slate-400 hover:text-slate-600 hover:bg-slate-50">
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
                                {/* Future options can be added here */}
                            </DropdownMenuContent>
                        </DropdownMenu>

                        <div className="h-6 w-px bg-slate-200 mx-1"></div>

                        <Link to="/create">
                            <Button className="bg-brand-blue hover:bg-brand-blue-hover text-white font-medium shadow-sm shadow-blue-200">
                                <PlusCircle className="mr-2 h-4 w-4" />
                                New Memo
                            </Button>
                        </Link>
                    </div>
                </header>

                {/* Page Content */}
                <main className="flex-1 overflow-x-hidden p-6 md:p-8">
                    <Outlet />
                </main>
            </div>
            <Toaster />
        </div>
    );
}
