import React from 'react';

export default function Badge({ children, type = 'neutral', split = false }) {
    // Map types to CSS classes
    const typeMap = {
        success: 'badge-success',
        warning: 'badge-warning',
        error: 'badge-error',
        info: 'badge-info', // We need to add this class
        neutral: 'badge-neutral',
        // permission specific
        READ: 'badge-success',
        WRITE: 'badge-warning',
        DELETE: 'badge-error',
        ADMIN: 'badge-neutral'
    };

    const className = typeMap[type] || typeMap[children] || 'badge-neutral';

    return (
        <span className={`badge ${className}`}>
            {children}
        </span>
    );
}
