import React, { useState } from 'react';
import { Button } from './ui/button';
import {
    UploadCloud, FileText, FileSpreadsheet, File, Image as ImageIcon,
    Download, Trash2, Loader2
} from 'lucide-react';
import FileUploader from './FileUploader';
import { MemoApi } from '../lib/api';
import { toast } from 'sonner';

export default function MemoAttachments({ memoId, attachments = [], onUpload, isEditable = false }) {

    const [downloading, setDownloading] = useState(null);
    const [deleting, setDeleting] = useState(null);

    const handleDownload = async (file) => {
        setDownloading(file.id);
        try {
            await MemoApi.downloadAttachment(memoId, file.id, file.fileName);
        } catch (error) {
            console.error('Download failed', error);
            toast.error('Failed to download file');
        } finally {
            setDownloading(null);
        }
    };

    const getFileIcon = (filename) => {
        const ext = filename.split('.').pop().toLowerCase();
        if (['pdf'].includes(ext)) return <FileText className="h-8 w-8 text-red-500" />;
        if (['xlsx', 'xls', 'csv'].includes(ext)) return <FileSpreadsheet className="h-8 w-8 text-green-600" />;
        if (['doc', 'docx'].includes(ext)) return <FileText className="h-8 w-8 text-blue-500" />;
        if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext)) return <ImageIcon className="h-8 w-8 text-purple-500" />;
        return <File className="h-8 w-8 text-slate-400" />;
    };

    const formatFileSize = (bytes) => {
        if (!bytes) return '—';
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    };

    const handleDelete = async (file) => {
        if (!window.confirm(`Delete "${file.fileName}"? This action cannot be undone.`)) return;
        setDeleting(file.id);
        try {
            await MemoApi.deleteAttachment(memoId, file.id);
            toast.success('Attachment deleted');
            onUpload(); // Refresh list
        } catch (error) {
            console.error('Delete failed', error);
            toast.error('Failed to delete attachment');
        } finally {
            setDeleting(null);
        }
    };

    return (
        <div className="space-y-8">
            {/* Upload Zone */}
            {isEditable && (
                <div className="border-2 border-dashed border-slate-200 rounded-xl bg-slate-50/50 p-12 text-center hover:bg-slate-50 transition-colors">
                    <div className="flex flex-col items-center justify-center space-y-4">
                        <div className="h-16 w-16 rounded-full bg-white shadow-sm flex items-center justify-center text-slate-400">
                            <UploadCloud className="h-8 w-8" />
                        </div>
                        <div className="space-y-1">
                            <h3 className="text-lg font-semibold text-slate-900">Upload New Attachment</h3>
                            <div className="text-sm text-slate-500">
                                <FileUploader
                                    memoId={memoId}
                                    onUploadComplete={onUpload}
                                    customTrigger={<span className="text-brand-blue font-medium hover:underline cursor-pointer">Click to browse</span>}
                                />
                                <span className="mx-1">or drag and drop files here.</span>
                            </div>
                            <p className="text-xs text-slate-400 pt-2">Supported formats: PDF, XLSX, DOCX, Images. Max file size: 25MB.</p>
                        </div>
                    </div>
                </div>
            )}

            {/* Attachments Table */}
            <div className="rounded-lg border border-slate-200 bg-white shadow-sm overflow-hidden">
                <table className="w-full text-left text-sm">
                    <thead>
                        <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 font-medium">
                            <th className="px-6 py-4">File Type</th>
                            <th className="px-6 py-4">Filename</th>
                            <th className="px-6 py-4">Size</th>
                            <th className="px-6 py-4">Uploaded By</th>
                            <th className="px-6 py-4">Timestamp</th>
                            <th className="px-6 py-4 text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {attachments.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="px-6 py-12 text-center text-slate-400 italic">No attachments found.</td>
                            </tr>
                        ) : (
                            attachments.map((file) => (
                                <tr key={file.id} className="hover:bg-slate-50/50 transition-colors group">
                                    <td className="px-6 py-4 text-center w-24">
                                        <div className="flex justify-center p-2 bg-white rounded-lg border border-slate-100 shadow-sm">
                                            {getFileIcon(file.fileName)}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="font-medium text-slate-900">{file.fileName}</div>
                                    </td>
                                    <td className="px-6 py-4 text-slate-500">
                                        {formatFileSize(file.size)}
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className="text-slate-700">{file.uploadedByName || 'Unknown'}</span>
                                    </td>
                                    <td className="px-6 py-4 text-slate-500">
                                        {new Date(file.createdAt || Date.now()).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' })}
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex items-center justify-end gap-2">
                                            <button
                                                onClick={() => handleDownload(file)}
                                                disabled={downloading === file.id}
                                                className="inline-flex items-center px-2.5 py-1.5 text-xs font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-md transition-colors disabled:opacity-50">
                                                {downloading === file.id ? <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" /> : <Download className="h-3.5 w-3.5 mr-1" />}
                                                {downloading === file.id ? 'Downloading...' : 'Download'}
                                            </button>
                                            {isEditable && (
                                                <Button variant="ghost" size="icon"
                                                    className="h-8 w-8 text-slate-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all"
                                                    disabled={deleting === file.id}
                                                    onClick={() => handleDelete(file)}>
                                                    {deleting === file.id ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                                                </Button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
