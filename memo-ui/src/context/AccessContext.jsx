import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import { MemoApi } from '../lib/api';

/**
 * AccessContext — stores resolved PRODUCT.MODULE.ACTION permissions.
 * Fetched from GET /api/me after login.
 * Frontend never sees roles — only resolved actions.
 */
const AccessContext = createContext(null);

export const AccessProvider = ({ children }) => {
    const [accessData, setAccessData] = useState(null);
    const [accessLoading, setAccessLoading] = useState(false);
    const accessKeysRef = useRef(new Set());

    /**
     * Build a flat Set of "PRODUCT.MODULE.ACTION" keys from the /api/me response.
     * Example: { MMS: { MEMO: ["CREATE","VIEW"] } } → Set("MMS.MEMO.CREATE", "MMS.MEMO.VIEW")
     */
    const buildAccessKeys = useCallback((data) => {
        const keys = new Set();
        if (data?.products) {
            for (const product of data.products) {
                for (const mod of product.modules || []) {
                    for (const action of mod.permissions || []) {
                        keys.add(`${product.code}.${mod.code}.${action}`);
                    }
                    // Also add module-level key for hasModule checks
                    keys.add(`${product.code}.${mod.code}`);
                }
            }
        }
        return keys;
    }, []);

    /**
     * Fetch effective access from /api/me. Called after login.
     */
    const fetchAccess = useCallback(async () => {
        setAccessLoading(true);
        try {
            const data = await MemoApi.getMe();
            setAccessData(data);
            accessKeysRef.current = buildAccessKeys(data);
            return data;
        } catch (error) {
            console.error('Failed to fetch access context:', error);
            setAccessData(null);
            accessKeysRef.current = new Set();
            return null;
        } finally {
            setAccessLoading(false);
        }
    }, [buildAccessKeys]);

    /**
     * Clear access data (on logout).
     */
    const clearAccess = useCallback(() => {
        setAccessData(null);
        accessKeysRef.current = new Set();
    }, []);

    /**
     * Check if the user has a specific PRODUCT.MODULE.ACTION permission.
     * e.g., has("MMS.MEMO.CREATE")
     */
    const has = useCallback((key) => {
        return accessKeysRef.current.has(key);
    }, []);

    /**
     * Check if the user has ANY of the given permissions.
     * e.g., hasAny(["MMS.MEMO.APPROVE", "MMS.MEMO.REJECT"])
     */
    const hasAny = useCallback((keys) => {
        if (typeof keys === 'string') {
            // Module-level check: "MMS.REPORT" → check if any key starts with it
            return Array.from(accessKeysRef.current).some(k => k.startsWith(keys + '.'));
        }
        return keys.some(key => accessKeysRef.current.has(key));
    }, []);

    /**
     * Check if the user has ALL of the given permissions.
     */
    const hasAll = useCallback((keys) => {
        return keys.every(key => accessKeysRef.current.has(key));
    }, []);

    /**
     * Check if the user has any permission in a module.
     * e.g., hasModule("MMS.REPORT") → true if any REPORT action exists
     */
    const hasModule = useCallback((moduleKey) => {
        return accessKeysRef.current.has(moduleKey);
    }, []);

    /**
     * Get constraints for a product.
     * e.g., constraints("MMS") → { branchIds: ["KTM-001"] }
     */
    const constraints = useCallback((productCode) => {
        if (!accessData?.products) return {};
        const product = accessData.products.find(p => p.code === productCode);
        return product?.constraints || {};
    }, [accessData]);

    const value = {
        accessData,
        accessLoading,
        fetchAccess,
        clearAccess,
        has,
        hasAny,
        hasAll,
        hasModule,
        constraints,
    };

    return (
        <AccessContext.Provider value={value}>
            {children}
        </AccessContext.Provider>
    );
};

export const useAccess = () => {
    const context = useContext(AccessContext);
    if (!context) {
        throw new Error('useAccess must be used within an AccessProvider');
    }
    return context;
};

export default AccessContext;
