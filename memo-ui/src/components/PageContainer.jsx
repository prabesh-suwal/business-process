import React from 'react';
import { cn } from '../lib/utils';

export const PageContainer = ({ children, className }) => {
    return (
        <div className={cn("space-y-6 w-full max-w-full animate-in fade-in duration-500", className)}>
            {children}
        </div>
    );
};
