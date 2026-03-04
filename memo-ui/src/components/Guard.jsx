import React from 'react';
import { useAccess } from '../context/AccessContext';

/**
 * <Guard> component — declarative access control for UI elements.
 * 
 * Props:
 *   access    - Single PRODUCT.MODULE.ACTION key to check
 *   accessAny - Array of keys, passes if user has ANY
 *   accessAll - Array of keys, passes if user has ALL
 *   module    - Module key (e.g., "MMS.REPORT"), passes if any action exists
 *   fallback  - Optional JSX to render when access is denied (default: null)
 *   children  - Content to show when access is granted
 * 
 * Usage:
 *   <Guard access="MMS.MEMO.CREATE"><CreateButton /></Guard>
 *   <Guard module="MMS.REPORT"><ReportsNav /></Guard>
 *   <Guard access="MMS.CONFIG.MANAGE" fallback={<ReadOnly />}><Form /></Guard>
 *   <Guard accessAny={["MMS.MEMO.APPROVE","MMS.MEMO.REJECT"]}><Actions /></Guard>
 */
const Guard = ({ access, accessAny, accessAll, module, fallback = null, children }) => {
    const { has, hasAny, hasAll, hasModule, accessLoading } = useAccess();

    // Don't render anything while access is still loading
    if (accessLoading) return fallback;

    let allowed = false;

    if (access) {
        allowed = has(access);
    } else if (accessAny) {
        allowed = hasAny(accessAny);
    } else if (accessAll) {
        allowed = hasAll(accessAll);
    } else if (module) {
        allowed = hasModule(module);
    } else {
        // No guard condition specified — allow by default
        allowed = true;
    }

    return allowed ? children : fallback;
};

/**
 * <GuardedRoute> — wraps a route element with access control.
 * If access is denied, shows an "Access Denied" message instead of the page.
 * 
 * Usage in App.jsx:
 *   <Route path="create" element={<GuardedRoute access="MMS.MEMO.CREATE"><CreateMemo /></GuardedRoute>} />
 */
export const GuardedRoute = ({ access, accessAny, accessAll, module, children }) => {
    const { has, hasAny, hasAll, hasModule, accessLoading } = useAccess();

    if (accessLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-muted-foreground">Loading...</div>
            </div>
        );
    }

    let allowed = false;

    if (access) {
        allowed = has(access);
    } else if (accessAny) {
        allowed = hasAny(accessAny);
    } else if (accessAll) {
        allowed = hasAll(accessAll);
    } else if (module) {
        allowed = hasModule(module);
    } else {
        allowed = true;
    }

    if (!allowed) {
        return (
            <div className="flex flex-col items-center justify-center h-64 gap-3">
                <div className="text-2xl font-semibold text-destructive">Access Denied</div>
                <p className="text-muted-foreground text-sm">
                    You don't have permission to access this page.
                </p>
            </div>
        );
    }

    return children;
};

export default Guard;
