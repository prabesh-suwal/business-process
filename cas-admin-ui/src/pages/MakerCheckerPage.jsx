import { useState, useEffect, useRef, useCallback } from 'react'
import { makerChecker, products, users } from '../api'

export default function MakerCheckerPage() {
    const [productList, setProductList] = useState([]);
    const [selectedProductId, setSelectedProductId] = useState(null);
    const [configs, setConfigs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [editConfig, setEditConfig] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [saving, setSaving] = useState(false);

    // Modal form state
    const [form, setForm] = useState({
        sameMakerCanCheck: false,
        deadlineHours: 24,
        escalationRole: '',
        autoExpire: true,
        checkerUserIds: [],
    });

    // Checker user search state
    const [checkerSearch, setCheckerSearch] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedCheckers, setSelectedCheckers] = useState([]);
    const [searching, setSearching] = useState(false);
    const [showDropdown, setShowDropdown] = useState(false);
    const searchRef = useRef(null);
    const dropdownRef = useRef(null);

    // Collapsed groups
    const [collapsedGroups, setCollapsedGroups] = useState({});

    // Load products on mount
    useEffect(() => {
        loadProducts();
    }, []);

    // Load configs when product changes
    useEffect(() => {
        if (selectedProductId) loadConfigs(selectedProductId);
    }, [selectedProductId]);

    // Click outside to close dropdown
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target) &&
                searchRef.current && !searchRef.current.contains(e.target)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const loadProducts = async () => {
        try {
            const list = await products.list();
            setProductList(list || []);
            if (list && list.length > 0) {
                setSelectedProductId(list[0].id);
            }
        } catch (err) {
            setError(err.message);
        }
    };

    const loadConfigs = async (productId) => {
        setLoading(true);
        setError('');
        try {
            const list = await makerChecker.configs(productId);
            setConfigs(list || []);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleToggleEnabled = async (config) => {
        try {
            await makerChecker.updateConfig(config.id, {
                productId: config.productId,
                serviceName: config.serviceName,
                endpointPattern: config.endpointPattern,
                httpMethod: config.httpMethod,
                endpointGroup: config.endpointGroup,
                description: config.description,
                sameMakerCanCheck: config.sameMakerCanCheck,
                enabled: !config.enabled,
                deadlineHours: config.deadlineHours,
                escalationRole: config.escalationRole,
                autoExpire: config.autoExpire,
                checkerUserIds: config.checkerUserIds,
            });
            loadConfigs(selectedProductId);
        } catch (err) {
            setError(err.message);
        }
    };

    const openSettings = async (config) => {
        setEditConfig(config);
        setForm({
            sameMakerCanCheck: config.sameMakerCanCheck,
            deadlineHours: config.deadlineHours || 24,
            escalationRole: config.escalationRole || '',
            autoExpire: config.autoExpire !== false,
            checkerUserIds: config.checkerUserIds || [],
        });

        // Load checker user details
        if (config.checkerUserIds && config.checkerUserIds.length > 0) {
            try {
                const checkerDetails = await Promise.all(
                    config.checkerUserIds.map(userId => users.get(userId))
                );
                setSelectedCheckers(checkerDetails.filter(Boolean));
            } catch {
                setSelectedCheckers([]);
            }
        } else {
            setSelectedCheckers([]);
        }

        setCheckerSearch('');
        setSearchResults([]);
        setShowDropdown(false);
        setShowModal(true);
    };

    // Debounced user search
    const searchTimeout = useRef(null);
    const handleSearchUsers = useCallback((query) => {
        setCheckerSearch(query);
        if (searchTimeout.current) clearTimeout(searchTimeout.current);

        if (!query || query.length < 2) {
            setSearchResults([]);
            setShowDropdown(false);
            return;
        }

        searchTimeout.current = setTimeout(async () => {
            setSearching(true);
            try {
                const result = await users.list(0, 10, query);
                const items = result?.content || result || [];
                // Filter out already-selected users
                const filtered = items.filter(
                    u => !selectedCheckers.some(sc => sc.id === u.id)
                );
                setSearchResults(filtered);
                setShowDropdown(true);
            } catch {
                setSearchResults([]);
            } finally {
                setSearching(false);
            }
        }, 300);
    }, [selectedCheckers]);

    const addChecker = (user) => {
        if (!selectedCheckers.some(sc => sc.id === user.id)) {
            const updated = [...selectedCheckers, user];
            setSelectedCheckers(updated);
            setForm(prev => ({ ...prev, checkerUserIds: updated.map(u => u.id) }));
        }
        setCheckerSearch('');
        setSearchResults([]);
        setShowDropdown(false);
    };

    const removeChecker = (userId) => {
        const updated = selectedCheckers.filter(u => u.id !== userId);
        setSelectedCheckers(updated);
        setForm(prev => ({ ...prev, checkerUserIds: updated.map(u => u.id) }));
    };

    const handleSaveSettings = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        try {
            await makerChecker.updateConfig(editConfig.id, {
                productId: editConfig.productId,
                serviceName: editConfig.serviceName,
                endpointPattern: editConfig.endpointPattern,
                httpMethod: editConfig.httpMethod,
                endpointGroup: editConfig.endpointGroup,
                description: editConfig.description,
                enabled: editConfig.enabled,
                sameMakerCanCheck: form.sameMakerCanCheck,
                deadlineHours: form.deadlineHours,
                escalationRole: form.escalationRole || null,
                autoExpire: form.autoExpire,
                checkerUserIds: form.checkerUserIds,
            });
            setShowModal(false);
            setEditConfig(null);
            loadConfigs(selectedProductId);
        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    };

    const toggleGroup = (group) => {
        setCollapsedGroups(prev => ({ ...prev, [group]: !prev[group] }));
    };

    // Group configs by endpointGroup
    const groupedConfigs = configs.reduce((acc, config) => {
        const group = config.endpointGroup || 'Other';
        if (!acc[group]) acc[group] = [];
        acc[group].push(config);
        return acc;
    }, {});

    const enabledCount = configs.filter(c => c.enabled).length;

    const methodColors = {
        POST: 'badge-success',
        PUT: 'badge-warning',
        PATCH: 'badge-info',
        DELETE: 'badge-error',
    };

    const groupIcons = {
        'User Management': '👥',
        'Role Management': '🛡️',
        'Memo Management': '📝',
        'Other': '📋',
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1>Maker-Checker Configuration</h1>
                    <p className="text-muted" style={{ marginTop: '4px', fontSize: '13px' }}>
                        Configure which endpoints require approval before execution
                    </p>
                </div>
            </div>

            {error && <div className="form-error mb-4">{error}</div>}

            {/* Product Tabs */}
            <div style={{
                display: 'flex',
                gap: '0',
                borderBottom: '2px solid var(--color-gray-200)',
                marginBottom: '24px',
                overflowX: 'auto',
            }}>
                {productList.map(product => (
                    <button
                        key={product.id}
                        onClick={() => setSelectedProductId(product.id)}
                        style={{
                            padding: '10px 20px',
                            fontSize: '14px',
                            fontWeight: selectedProductId === product.id ? '600' : '400',
                            color: selectedProductId === product.id ? 'var(--color-primary-600)' : 'var(--color-gray-500)',
                            background: 'none',
                            border: 'none',
                            borderBottom: selectedProductId === product.id ? '2px solid var(--color-primary-600)' : '2px solid transparent',
                            marginBottom: '-2px',
                            cursor: 'pointer',
                            transition: 'all 150ms ease',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {product.name}
                        <span style={{
                            marginLeft: '8px',
                            fontSize: '11px',
                            padding: '1px 6px',
                            borderRadius: '10px',
                            background: selectedProductId === product.id ? 'var(--color-primary-100)' : 'var(--color-gray-100)',
                            color: selectedProductId === product.id ? 'var(--color-primary-700)' : 'var(--color-gray-500)',
                        }}>
                            {product.code}
                        </span>
                    </button>
                ))}
            </div>

            {/* Stats Summary */}
            {!loading && (
                <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)', marginBottom: '20px' }}>
                    <div className="stat-card">
                        <div className="stat-label">Total Endpoints</div>
                        <div className="stat-value">{configs.length}</div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-label">Approval Required</div>
                        <div className="stat-value" style={{ color: 'var(--color-success)' }}>{enabledCount}</div>
                    </div>
                    <div className="stat-card">
                        <div className="stat-label">Disabled</div>
                        <div className="stat-value" style={{ color: 'var(--color-gray-400)' }}>{configs.length - enabledCount}</div>
                    </div>
                </div>
            )}

            {/* Grouped Config Tables */}
            {loading ? (
                <div className="card"><div className="loading">Loading configurations...</div></div>
            ) : configs.length === 0 ? (
                <div className="card">
                    <div className="empty-state">
                        <p>No endpoints configured for this product</p>
                        <p className="text-muted">Endpoints will appear here once configured</p>
                    </div>
                </div>
            ) : (
                Object.entries(groupedConfigs).map(([group, groupConfigs]) => (
                    <div key={group} className="card" style={{ marginBottom: '16px' }}>
                        {/* Group Header */}
                        <div
                            onClick={() => toggleGroup(group)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                padding: '14px 20px',
                                cursor: 'pointer',
                                borderBottom: collapsedGroups[group] ? 'none' : '1px solid var(--color-gray-200)',
                                userSelect: 'none',
                            }}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span style={{ fontSize: '18px' }}>{groupIcons[group] || '📋'}</span>
                                <span style={{ fontWeight: '600', fontSize: '15px' }}>{group}</span>
                                <span style={{
                                    fontSize: '12px',
                                    padding: '2px 8px',
                                    borderRadius: '10px',
                                    background: 'var(--color-gray-100)',
                                    color: 'var(--color-gray-600)',
                                }}>
                                    {groupConfigs.length} endpoint{groupConfigs.length !== 1 ? 's' : ''}
                                </span>
                                <span style={{
                                    fontSize: '12px',
                                    padding: '2px 8px',
                                    borderRadius: '10px',
                                    background: groupConfigs.some(c => c.enabled) ? 'var(--color-success-light, #dcfce7)' : 'var(--color-gray-100)',
                                    color: groupConfigs.some(c => c.enabled) ? 'var(--color-success, #16a34a)' : 'var(--color-gray-500)',
                                }}>
                                    {groupConfigs.filter(c => c.enabled).length} active
                                </span>
                            </div>
                            <span style={{
                                transform: collapsedGroups[group] ? 'rotate(-90deg)' : 'rotate(0deg)',
                                transition: 'transform 200ms ease',
                                fontSize: '14px',
                                color: 'var(--color-gray-400)',
                            }}>▼</span>
                        </div>

                        {/* Group Table */}
                        {!collapsedGroups[group] && (
                            <div className="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th style={{ width: '120px' }}>Approval</th>
                                            <th style={{ width: '80px' }}>Method</th>
                                            <th>Endpoint Pattern</th>
                                            <th>Description</th>
                                            <th style={{ width: '100px' }}>Checkers</th>
                                            <th style={{ width: '80px' }}>SLA</th>
                                            <th style={{ width: '100px' }}>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {groupConfigs.map(config => (
                                            <tr key={config.id}>
                                                <td>
                                                    <label style={{
                                                        position: 'relative',
                                                        display: 'inline-block',
                                                        width: '44px',
                                                        height: '24px',
                                                        cursor: 'pointer',
                                                    }}>
                                                        <input
                                                            type="checkbox"
                                                            checked={config.enabled}
                                                            onChange={() => handleToggleEnabled(config)}
                                                            style={{ opacity: 0, width: 0, height: 0 }}
                                                        />
                                                        <span style={{
                                                            position: 'absolute',
                                                            inset: 0,
                                                            borderRadius: '12px',
                                                            background: config.enabled ? 'var(--color-success)' : 'var(--color-gray-300)',
                                                            transition: 'background 200ms ease',
                                                        }} />
                                                        <span style={{
                                                            position: 'absolute',
                                                            top: '2px',
                                                            left: config.enabled ? '22px' : '2px',
                                                            width: '20px',
                                                            height: '20px',
                                                            borderRadius: '50%',
                                                            background: 'white',
                                                            boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
                                                            transition: 'left 200ms ease',
                                                        }} />
                                                    </label>
                                                </td>
                                                <td>
                                                    <span className={`badge ${methodColors[config.httpMethod] || 'badge-neutral'}`}>
                                                        {config.httpMethod}
                                                    </span>
                                                </td>
                                                <td>
                                                    <code style={{ fontSize: '12px', background: 'var(--color-gray-50)', padding: '2px 6px', borderRadius: '4px' }}>
                                                        {config.endpointPattern}
                                                    </code>
                                                </td>
                                                <td>{config.description || '—'}</td>
                                                <td>
                                                    <span className={`badge ${(config.checkerUserIds?.length > 0) ? 'badge-info' : 'badge-neutral'}`}>
                                                        {config.checkerUserIds?.length || 0} user{(config.checkerUserIds?.length || 0) !== 1 ? 's' : ''}
                                                    </span>
                                                </td>
                                                <td>{config.deadlineHours || '24'}h</td>
                                                <td>
                                                    <button
                                                        className="btn btn-sm btn-secondary"
                                                        onClick={() => openSettings(config)}
                                                    >
                                                        ⚙️ Settings
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                ))
            )}

            {/* Settings Modal */}
            {showModal && editConfig && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '560px' }}>
                        <div className="modal-header">
                            <div>
                                <h3>Endpoint Settings</h3>
                                <div className="text-muted" style={{ fontSize: '12px', marginTop: '4px' }}>
                                    <span className={`badge ${methodColors[editConfig.httpMethod] || 'badge-neutral'}`} style={{ marginRight: '6px' }}>
                                        {editConfig.httpMethod}
                                    </span>
                                    <code>{editConfig.endpointPattern}</code>
                                </div>
                            </div>
                            <button className="btn btn-sm btn-secondary" onClick={() => setShowModal(false)}>✕</button>
                        </div>
                        <form onSubmit={handleSaveSettings}>
                            <div className="modal-body">
                                {error && <div className="form-error mb-4">{error}</div>}

                                {/* Same Maker Can Check */}
                                <div className="form-group">
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        padding: '12px',
                                        background: 'var(--color-gray-50)',
                                        borderRadius: '6px',
                                        border: '1px solid var(--color-gray-200)',
                                    }}>
                                        <div>
                                            <label className="form-label" style={{ marginBottom: 0 }}>Same Maker Can Check</label>
                                            <div className="text-muted" style={{ fontSize: '12px' }}>
                                                Allow the same user who submitted to also approve
                                            </div>
                                        </div>
                                        <ToggleSwitch
                                            checked={form.sameMakerCanCheck}
                                            onChange={(v) => setForm({ ...form, sameMakerCanCheck: v })}
                                        />
                                    </div>
                                </div>

                                {/* Checker Users */}
                                <div className="form-group">
                                    <label className="form-label">Checker Users (Approvers)</label>
                                    <div className="text-muted" style={{ fontSize: '12px', marginBottom: '8px' }}>
                                        Users authorized to approve requests for this endpoint
                                    </div>

                                    {/* Selected checker chips */}
                                    {selectedCheckers.length > 0 && (
                                        <div style={{
                                            display: 'flex',
                                            flexWrap: 'wrap',
                                            gap: '6px',
                                            marginBottom: '8px',
                                        }}>
                                            {selectedCheckers.map(user => (
                                                <span key={user.id} style={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    gap: '6px',
                                                    padding: '4px 10px',
                                                    borderRadius: '16px',
                                                    background: 'var(--color-primary-50, #eff6ff)',
                                                    border: '1px solid var(--color-primary-200, #bfdbfe)',
                                                    fontSize: '13px',
                                                    color: 'var(--color-primary-700, #1d4ed8)',
                                                }}>
                                                    <span style={{ width: '22px', height: '22px', borderRadius: '50%', background: 'var(--color-primary-200, #bfdbfe)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '11px', fontWeight: '600', color: 'var(--color-primary-700, #1d4ed8)' }}>
                                                        {(user.name || user.email || '?').charAt(0).toUpperCase()}
                                                    </span>
                                                    <span>
                                                        <strong>{user.name || 'Unknown'}</strong>
                                                        {user.email && <span style={{ color: 'var(--color-gray-500)', marginLeft: '4px', fontSize: '11px' }}>{user.email}</span>}
                                                    </span>
                                                    <button
                                                        type="button"
                                                        onClick={() => removeChecker(user.id)}
                                                        style={{
                                                            background: 'none',
                                                            border: 'none',
                                                            cursor: 'pointer',
                                                            fontSize: '14px',
                                                            color: 'var(--color-gray-400)',
                                                            padding: '0 2px',
                                                            lineHeight: 1,
                                                        }}
                                                        title="Remove"
                                                    >×</button>
                                                </span>
                                            ))}
                                        </div>
                                    )}

                                    {/* User search input */}
                                    <div style={{ position: 'relative' }}>
                                        <input
                                            ref={searchRef}
                                            type="text"
                                            className="form-input"
                                            placeholder="Search users by name or email..."
                                            value={checkerSearch}
                                            onChange={(e) => handleSearchUsers(e.target.value)}
                                            onFocus={() => { if (searchResults.length > 0) setShowDropdown(true); }}
                                        />
                                        {searching && (
                                            <span style={{
                                                position: 'absolute',
                                                right: '10px',
                                                top: '50%',
                                                transform: 'translateY(-50%)',
                                                fontSize: '12px',
                                                color: 'var(--color-gray-400)',
                                            }}>Searching...</span>
                                        )}

                                        {/* Search results dropdown */}
                                        {showDropdown && searchResults.length > 0 && (
                                            <div ref={dropdownRef} style={{
                                                position: 'absolute',
                                                top: '100%',
                                                left: 0,
                                                right: 0,
                                                maxHeight: '220px',
                                                overflowY: 'auto',
                                                background: 'white',
                                                border: '1px solid var(--color-gray-200)',
                                                borderRadius: '6px',
                                                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                                zIndex: 1000,
                                                marginTop: '4px',
                                            }}>
                                                {searchResults.map(user => (
                                                    <div
                                                        key={user.id}
                                                        onClick={() => addChecker(user)}
                                                        style={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            gap: '10px',
                                                            padding: '10px 14px',
                                                            cursor: 'pointer',
                                                            borderBottom: '1px solid var(--color-gray-100)',
                                                            transition: 'background 100ms ease',
                                                        }}
                                                        onMouseEnter={(e) => e.currentTarget.style.background = 'var(--color-gray-50)'}
                                                        onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                                                    >
                                                        <span style={{
                                                            width: '32px',
                                                            height: '32px',
                                                            borderRadius: '50%',
                                                            background: 'var(--color-primary-100, #dbeafe)',
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            justifyContent: 'center',
                                                            fontSize: '13px',
                                                            fontWeight: '600',
                                                            color: 'var(--color-primary-700, #1d4ed8)',
                                                            flexShrink: 0,
                                                        }}>
                                                            {(user.name || user.email || '?').charAt(0).toUpperCase()}
                                                        </span>
                                                        <div style={{ flex: 1, minWidth: 0 }}>
                                                            <div style={{ fontWeight: '500', fontSize: '13px' }}>{user.name || 'Unknown'}</div>
                                                            <div style={{ fontSize: '12px', color: 'var(--color-gray-500)' }}>{user.email || ''}</div>
                                                        </div>
                                                        {user.roles && user.roles.length > 0 && (
                                                            <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                                                                {user.roles.slice(0, 2).map((role, i) => (
                                                                    <span key={i} style={{
                                                                        fontSize: '10px',
                                                                        padding: '1px 6px',
                                                                        borderRadius: '8px',
                                                                        background: 'var(--color-gray-100)',
                                                                        color: 'var(--color-gray-600)',
                                                                        whiteSpace: 'nowrap',
                                                                    }}>{typeof role === 'string' ? role : role.roleName || role.name}</span>
                                                                ))}
                                                                {user.roles.length > 2 && (
                                                                    <span style={{ fontSize: '10px', color: 'var(--color-gray-400)' }}>+{user.roles.length - 2}</span>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                        {showDropdown && searchResults.length === 0 && checkerSearch.length >= 2 && !searching && (
                                            <div ref={dropdownRef} style={{
                                                position: 'absolute',
                                                top: '100%',
                                                left: 0,
                                                right: 0,
                                                padding: '12px 14px',
                                                background: 'white',
                                                border: '1px solid var(--color-gray-200)',
                                                borderRadius: '6px',
                                                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                                                zIndex: 1000,
                                                marginTop: '4px',
                                                color: 'var(--color-gray-500)',
                                                fontSize: '13px',
                                            }}>
                                                No users found matching "{checkerSearch}"
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* SLA Settings */}
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                    <div className="form-group">
                                        <label className="form-label">SLA Deadline (hours)</label>
                                        <input
                                            type="number"
                                            className="form-input"
                                            min="1"
                                            max="720"
                                            value={form.deadlineHours}
                                            onChange={(e) => setForm({ ...form, deadlineHours: parseInt(e.target.value) || 24 })}
                                        />
                                        <small className="text-muted">Request expires after this time</small>
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Escalation Role</label>
                                        <input
                                            className="form-input"
                                            value={form.escalationRole}
                                            onChange={(e) => setForm({ ...form, escalationRole: e.target.value })}
                                            placeholder="e.g., SUPERVISOR"
                                        />
                                        <small className="text-muted">Role notified on SLA breach</small>
                                    </div>
                                </div>

                                {/* Auto-expire */}
                                <div className="form-group">
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        padding: '12px',
                                        background: 'var(--color-gray-50)',
                                        borderRadius: '6px',
                                        border: '1px solid var(--color-gray-200)',
                                    }}>
                                        <div>
                                            <label className="form-label" style={{ marginBottom: 0 }}>Auto-Expire</label>
                                            <div className="text-muted" style={{ fontSize: '12px' }}>
                                                Automatically expire after SLA deadline
                                            </div>
                                        </div>
                                        <ToggleSwitch
                                            checked={form.autoExpire}
                                            onChange={(v) => setForm({ ...form, autoExpire: v })}
                                        />
                                    </div>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary" disabled={saving}>
                                    {saving ? 'Saving...' : 'Save Settings'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

// Reusable toggle switch component
function ToggleSwitch({ checked, onChange }) {
    return (
        <label style={{
            position: 'relative',
            display: 'inline-block',
            width: '44px',
            height: '24px',
            cursor: 'pointer',
            flexShrink: 0,
        }}>
            <input
                type="checkbox"
                checked={checked}
                onChange={(e) => onChange(e.target.checked)}
                style={{ opacity: 0, width: 0, height: 0 }}
            />
            <span style={{
                position: 'absolute', inset: 0, borderRadius: '12px',
                background: checked ? 'var(--color-primary-600)' : 'var(--color-gray-300)',
                transition: 'background 200ms ease',
            }} />
            <span style={{
                position: 'absolute', top: '2px',
                left: checked ? '22px' : '2px',
                width: '20px', height: '20px', borderRadius: '50%',
                background: 'white',
                boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
                transition: 'left 200ms ease',
            }} />
        </label>
    );
}
