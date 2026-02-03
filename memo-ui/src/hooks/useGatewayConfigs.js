import { useState, useEffect, useCallback } from 'react';
import { MemoApi } from '../lib/api';

/**
 * Hook for managing gateway configurations for a topic.
 * Used by the workflow designer to load/save gateway completion modes.
 */
export function useGatewayConfigs(topicId) {
    const [configs, setConfigs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    // Fetch configs
    const fetchConfigs = useCallback(async () => {
        if (!topicId) {
            setConfigs([]);
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            const data = await MemoApi.getGatewayConfigs(topicId);
            setConfigs(data || []);
            setError(null);
        } catch (err) {
            console.warn('Error fetching gateway configs:', err);
            setConfigs([]);
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [topicId]);

    // Initial fetch
    useEffect(() => {
        fetchConfigs();
    }, [fetchConfigs]);

    // Get config for a specific gateway
    const getConfig = useCallback((gatewayId) => {
        return configs.find(c => c.gatewayId === gatewayId);
    }, [configs]);

    // Save a single gateway config
    const saveConfig = useCallback(async (gatewayId, config) => {
        if (!topicId) return null;

        try {
            setSaving(true);
            const saved = await MemoApi.saveGatewayConfig(topicId, gatewayId, {
                ...config,
                gatewayId
            });

            // Update local state
            setConfigs(prev => {
                const existing = prev.findIndex(c => c.gatewayId === gatewayId);
                if (existing >= 0) {
                    const updated = [...prev];
                    updated[existing] = saved;
                    return updated;
                }
                return [...prev, saved];
            });

            return saved;
        } catch (err) {
            console.error('Error saving gateway config:', err);
            setError(err);
            throw err;
        } finally {
            setSaving(false);
        }
    }, [topicId]);

    // Bulk save all configs
    const saveAllConfigs = useCallback(async (configsToSave) => {
        if (!topicId) return [];

        try {
            setSaving(true);
            const saved = await MemoApi.saveGatewayConfigs(topicId, configsToSave);
            setConfigs(saved);
            return saved;
        } catch (err) {
            console.error('Error saving gateway configs:', err);
            setError(err);
            throw err;
        } finally {
            setSaving(false);
        }
    }, [topicId]);

    // Delete a gateway config
    const deleteConfig = useCallback(async (gatewayId) => {
        if (!topicId) return;

        try {
            await MemoApi.deleteGatewayConfig(topicId, gatewayId);
            setConfigs(prev => prev.filter(c => c.gatewayId !== gatewayId));
        } catch (err) {
            console.error('Error deleting gateway config:', err);
            setError(err);
        }
    }, [topicId]);

    // Build a config map for quick lookup
    const configMap = configs.reduce((acc, config) => {
        acc[config.gatewayId] = config;
        return acc;
    }, {});

    return {
        configs,
        configMap,
        loading,
        saving,
        error,
        getConfig,
        saveConfig,
        saveAllConfigs,
        deleteConfig,
        refresh: fetchConfigs
    };
}

export default useGatewayConfigs;
