import React, { useState } from 'react';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import {
    UploadCloud, FileText, FileSpreadsheet, File, Eye, Download,
    MoreHorizontal, CheckCircle, Clock
} from 'lucide-react';
import { Badge } from './ui/badge';
import FileUploader from './FileUploader'; // Reusing for logic, but might need custom UI
import { MemoApi } from '../lib/api';

export default function MemoAttachments({ memoId, attachments = [], onUpload, isEditable = false }) {

    // Helper to determine icon based on file extension
    const getFileIcon = (filename) => {
        const ext = filename.split('.').pop().toLowerCase();
        if (['pdf'].includes(ext)) return <FileText className="h-8 w-8 text-red-500" />;
        if (['xlsx', 'xls', 'csv'].includes(ext)) return <FileSpreadsheet className="h-8 w-8 text-green-600" />;
        return <File className="h-8 w-8 text-slate-400" />;
    };

    // Helper for mock Process Stage (random assignment if not present)
    const getProcessStage = (index) => {
        const stages = ['Initial Review', 'Risk Assessment', 'Legal Review', 'Approved'];
        return stages[index % stages.length];
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
                            <p className="text-xs text-slate-400 pt-2">Supported formats: PDF, XLSX, DOCX. Max file size: 25MB.</p>
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
                            <th className="px-6 py-4">Uploaded By</th>
                            <th className="px-6 py-4">Timestamp</th>
                            <th className="px-6 py-4">Process Stage</th>
                            <th className="px-6 py-4 text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                        {attachments.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="px-6 py-12 text-center text-slate-400 italic">No attachments found.</td>
                            </tr>
                        ) : (
                            attachments.map((file, index) => (
                                <tr key={file.id} className="hover:bg-slate-50/50 transition-colors group">
                                    <td className="px-6 py-4 text-center w-24">
                                        <div className="flex justify-center p-2 bg-white rounded-lg border border-slate-100 shadow-sm">
                                            {getFileIcon(file.fileName)}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="font-medium text-slate-900">{file.fileName}</div>
                                        {/* <div className="text-xs text-slate-400">{(file.size / 1024).toFixed(1)} KB</div> */}
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className="text-slate-700">Sarah Jenkins</span> {/* Mock Data */}
                                    </td>
                                    <td className="px-6 py-4 text-slate-500">
                                        {new Date(file.createdAt || Date.now()).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' })}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-slate-700 font-medium">{getProcessStage(index)}</div>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex items-center justify-end gap-3">
                                            <a href={MemoApi.getAttachmentUrl(memoId, file.id)} target="_blank" rel="noreferrer" className="flex items-center text-slate-500 hover:text-brand-blue font-medium text-xs transition-colors">
                                                <Download className="h-4 w-4 mr-1.5" />
                                                Download
                                            </a>
                                            <button className="flex items-center text-slate-500 hover:text-brand-blue font-medium text-xs transition-colors">
                                                <Eye className="h-4 w-4 mr-1.5" />
                                                Preview
                                            </button>
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
