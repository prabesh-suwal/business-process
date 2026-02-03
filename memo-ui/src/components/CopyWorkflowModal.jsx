import React, { useState, useEffect } from 'react';
import { X, FolderPlus, FileText, Hash, Sparkles, Folder, Loader2 } from 'lucide-react';
import { MemoApi } from '../lib/api';

/**
 * Modal for copying a workflow to a new topic.
 * Collects category, topic name, code, and numbering pattern.
 * Creates the topic immediately on confirm.
 */
const CopyWorkflowModal = ({ isOpen, onClose, onTopicCreated, sourceTopic, sourceWorkflowXml }) => {
    const [name, setName] = useState('');
    const [code, setCode] = useState('');
    const [numberingPattern, setNumberingPattern] = useState('');
    const [categoryId, setCategoryId] = useState('');
    const [categories, setCategories] = useState([]);
    const [loadingCategories, setLoadingCategories] = useState(false);
    const [creating, setCreating] = useState(false);
    const [errors, setErrors] = useState({});

    // Load categories when modal opens
    useEffect(() => {
        if (isOpen) {
            loadCategories();
            // Pre-select category from source topic if available
            if (sourceTopic?.category?.id) {
                setCategoryId(sourceTopic.category.id);
            }
        }
    }, [isOpen, sourceTopic]);

    const loadCategories = async () => {
        setLoadingCategories(true);
        try {
            const data = await MemoApi.getCategories();
            setCategories(data || []);
        } catch (error) {
            console.error('Failed to load categories:', error);
        } finally {
            setLoadingCategories(false);
        }
    };

    if (!isOpen) return null;

    const validate = () => {
        const newErrors = {};
        if (!categoryId) newErrors.categoryId = 'Please select a category';
        if (!name.trim()) newErrors.name = 'Topic name is required';
        if (!code.trim()) newErrors.code = 'Topic code is required';
        if (!/^[A-Z_]+$/.test(code)) newErrors.code = 'Code must be uppercase letters and underscores only';
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validate()) return;

        setCreating(true);
        try {
            // Create the topic immediately
            const newTopic = await MemoApi.createTopic({
                categoryId,
                name: name.trim(),
                code: code.trim().toUpperCase(),
                numberingPattern: numberingPattern.trim() || `${code.trim().toUpperCase()}-{YYYY}-{SEQ}`
            });

            // If we have workflow XML to copy, save it to the new topic
            if (sourceWorkflowXml) {
                await MemoApi.updateTopicWorkflow(newTopic.id, sourceWorkflowXml);
            }

            // Callback with the newly created topic
            onTopicCreated(newTopic);

            // Reset form
            setName('');
            setCode('');
            setNumberingPattern('');
            setCategoryId('');
            setErrors({});
        } catch (error) {
            console.error('Failed to create topic:', error);
            setErrors({ submit: error.response?.data?.message || 'Failed to create topic' });
        } finally {
            setCreating(false);
        }
    };

    const handleCodeChange = (e) => {
        const value = e.target.value.toUpperCase().replace(/[^A-Z_]/g, '');
        setCode(value);
        // Auto-generate numbering pattern
        if (value && !numberingPattern) {
            setNumberingPattern(`${value}-{YYYY}-{SEQ}`);
        }
    };

    const handleClose = () => {
        if (!creating) {
            setName('');
            setCode('');
            setNumberingPattern('');
            setCategoryId('');
            setErrors({});
            onClose();
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={handleClose}
            />

            {/* Modal */}
            <div className="relative bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 bg-gradient-to-r from-blue-500/10 to-indigo-500/10">
                    <div className="flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-blue-500/20">
                            <FolderPlus className="w-5 h-5 text-blue-400" />
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold text-white">Copy as New Topic</h3>
                            <p className="text-xs text-slate-400">
                                From: {sourceTopic?.name || 'Unknown'}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={handleClose}
                        disabled={creating}
                        className="p-2 rounded-lg hover:bg-white/10 transition-colors disabled:opacity-50"
                    >
                        <X className="w-5 h-5 text-slate-400" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-6 space-y-5">
                    {/* Category Dropdown */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
                            <Folder className="w-4 h-4 text-slate-400" />
                            Category <span className="text-red-400">*</span>
                        </label>
                        {loadingCategories ? (
                            <div className="flex items-center gap-2 text-slate-400 text-sm py-2">
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Loading categories...
                            </div>
                        ) : (
                            <select
                                value={categoryId}
                                onChange={(e) => setCategoryId(e.target.value)}
                                className={`w-full px-4 py-2.5 bg-slate-800 border rounded-lg text-white focus:outline-none focus:ring-2 transition-all ${errors.categoryId
                                    ? 'border-red-500/50 focus:ring-red-500/30'
                                    : 'border-slate-600 focus:ring-blue-500/30 focus:border-blue-500/50'
                                    }`}
                            >
                                <option value="">Select a category...</option>
                                {categories.map((cat) => (
                                    <option key={cat.id} value={cat.id}>
                                        {cat.name}
                                    </option>
                                ))}
                            </select>
                        )}
                        {errors.categoryId && (
                            <p className="mt-1.5 text-xs text-red-400">{errors.categoryId}</p>
                        )}
                    </div>

                    {/* Topic Name */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
                            <FileText className="w-4 h-4 text-slate-400" />
                            Topic Name <span className="text-red-400">*</span>
                        </label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="e.g., Leave Request"
                            disabled={creating}
                            className={`w-full px-4 py-2.5 bg-slate-800 border rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 transition-all disabled:opacity-50 ${errors.name
                                ? 'border-red-500/50 focus:ring-red-500/30'
                                : 'border-slate-600 focus:ring-blue-500/30 focus:border-blue-500/50'
                                }`}
                        />
                        {errors.name && (
                            <p className="mt-1.5 text-xs text-red-400">{errors.name}</p>
                        )}
                    </div>

                    {/* Topic Code */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
                            <Hash className="w-4 h-4 text-slate-400" />
                            Topic Code <span className="text-red-400">*</span>
                        </label>
                        <input
                            type="text"
                            value={code}
                            onChange={handleCodeChange}
                            placeholder="e.g., LEAVE_REQ"
                            disabled={creating}
                            className={`w-full px-4 py-2.5 bg-slate-800 border rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 transition-all font-mono disabled:opacity-50 ${errors.code
                                ? 'border-red-500/50 focus:ring-red-500/30'
                                : 'border-slate-600 focus:ring-blue-500/30 focus:border-blue-500/50'
                                }`}
                        />
                        {errors.code && (
                            <p className="mt-1.5 text-xs text-red-400">{errors.code}</p>
                        )}
                        <p className="mt-1.5 text-xs text-slate-500">Uppercase letters and underscores only</p>
                    </div>

                    {/* Numbering Pattern */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-slate-300 mb-2">
                            <Sparkles className="w-4 h-4 text-slate-400" />
                            Numbering Pattern
                        </label>
                        <input
                            type="text"
                            value={numberingPattern}
                            onChange={(e) => setNumberingPattern(e.target.value)}
                            placeholder={`${code || 'CODE'}-{YYYY}-{SEQ}`}
                            disabled={creating}
                            className="w-full px-4 py-2.5 bg-slate-800 border border-slate-600 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500/50 transition-all font-mono disabled:opacity-50"
                        />
                        <p className="mt-1.5 text-xs text-slate-500">
                            Available: {'{YYYY}'}, {'{YY}'}, {'{MM}'}, {'{DD}'}, {'{SEQ}'}
                        </p>
                    </div>

                    {/* Info Box */}
                    <div className="p-4 rounded-lg bg-blue-500/10 border border-blue-500/20">
                        <p className="text-xs text-blue-300">
                            <strong>What gets copied:</strong> Workflow design will be copied to the new topic.
                        </p>
                        <p className="text-xs text-slate-400 mt-1">
                            The original topic remains unchanged. You can configure step settings after creation.
                        </p>
                    </div>

                    {/* Error message */}
                    {errors.submit && (
                        <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/30">
                            <p className="text-sm text-red-400">{errors.submit}</p>
                        </div>
                    )}

                    {/* Actions */}
                    <div className="flex gap-3 pt-2">
                        <button
                            type="button"
                            onClick={handleClose}
                            disabled={creating}
                            className="flex-1 px-4 py-2.5 rounded-lg border border-slate-600 text-slate-300 hover:bg-slate-800 transition-colors disabled:opacity-50"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={creating || loadingCategories}
                            className="flex-1 px-4 py-2.5 rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600 text-white font-medium hover:from-blue-600 hover:to-indigo-700 transition-all shadow-lg shadow-blue-500/25 disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                            {creating ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    Creating...
                                </>
                            ) : (
                                'Create & Open'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CopyWorkflowModal;
