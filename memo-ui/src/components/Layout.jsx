import React, { useEffect, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { FileText, PlusCircle, LayoutDashboard, Settings, LogOut, User, Menu, Eye } from 'lucide-react';
import { cn } from '../lib/utils';
import { MemoApi } from '../lib/api';
import { Toaster } from 'sonner';
import { Button } from './ui/button';

const NavItem = ({ to, icon: Icon, label }) => {
    const location = useLocation();
    const isActive = location.pathname === to || (to !== '/' && location.pathname.startsWith(to));

    return (
        <Link
            to={to}
            className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
        >
            <Icon className="h-4 w-4" />
            {label}
        </Link>
    );
};

import { useAuth } from '../context/AuthContext';

export default function Layout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    // No need for useEffect session check, ProtectedRoute handles it

    return (
        <div className="min-h-screen bg-background flex flex-col md:flex-row font-sans text-foreground">
            {/* Sidebar */}
            <aside className="hidden md:flex w-64 border-r bg-card flex-col fixed inset-y-0 z-50">
                <div className="p-6">
                    <div className="flex items-center gap-2 font-bold text-xl text-primary">
                        <div className="bg-primary text-primary-foreground p-1.5 rounded-lg">
                            <FileText className="h-5 w-5" />
                        </div>
                        <span>Memo Sys</span>
                    </div>
                </div>

                <div className="flex-1 px-4 py-2">
                    <nav className="flex flex-col gap-1 space-y-1">
                        <div className="px-3 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                            Menu
                        </div>
                        <NavItem to="/" icon={LayoutDashboard} label="Dashboard" />
                        <NavItem to="/create" icon={PlusCircle} label="New Memo" />
                        <NavItem to="/tasks" icon={FileText} label="Task Inbox" />
                        <NavItem to="/view-only-memos" icon={Eye} label="View-Only Memos" />
                        <div className="my-4 border-b border-border/50" />
                        <div className="px-3 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                            Settings
                        </div>
                        <NavItem to="/settings" icon={Settings} label="System Config" />
                    </nav>
                </div>

                <div className="p-4 mt-auto border-t bg-muted/10 space-y-4">
                    {user ? (
                        <div className="flex items-center gap-3 px-2">
                            <div className="h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center text-primary font-medium">
                                {user.username ? user.username[0].toUpperCase() : 'U'}
                            </div>
                            <div className="overflow-hidden">
                                <p className="text-sm font-medium truncate">{user.username}</p>
                                <p className="text-xs text-muted-foreground truncate" title={user.email}>
                                    {user.email || 'Admin User'}
                                </p>
                            </div>
                        </div>
                    ) : (
                        <div className="px-2 text-sm text-muted-foreground">Guest User</div>
                    )}

                    <Button
                        variant="ghost"
                        className="w-full justify-start text-destructive hover:text-destructive hover:bg-destructive/10"
                        onClick={logout}
                    >
                        <LogOut className="mr-2 h-4 w-4" />
                        Logout
                    </Button>
                </div>
            </aside>

            {/* Mobile Header (Visible only on small screens) */}
            <div className="md:hidden border-b bg-card p-4 flex items-center justify-between sticky top-0 z-50">
                <div className="flex items-center gap-2 font-bold text-lg">
                    <FileText className="h-5 w-5 text-primary" />
                    <span>Memo Sys</span>
                </div>
                <Button variant="ghost" size="icon"><Menu className="h-5 w-5" /></Button>
            </div>

            {/* Main Content */}
            <main className="flex-1 md:ml-64 bg-muted/5">
                <div className="h-full">
                    <Outlet />
                </div>
            </main>

            <Toaster position="top-right" />
        </div>
    );
}
