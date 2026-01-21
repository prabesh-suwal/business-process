import React, { useState, useRef } from 'react';
import { Button } from './ui/button';
import { Upload, X, File, Paperclip, Loader2 } from 'lucide-react';
import { MemoApi } from '../lib/api';

export default function FileUploader({ memoId, onUploadComplete }) {
    const [uploading, setUploading] = useState(false);
    const fileInputRef = useRef(null);

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setUploading(true);
        try {
            await MemoApi.uploadAttachment(memoId, file);
            onUploadComplete();
        } catch (error) {
            console.error("Upload failed", error);
            alert("Upload failed");
        } finally {
            setUploading(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    return (
        <div>
            <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                onChange={handleFileChange}
            />
            <Button
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
            >
                {uploading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Paperclip className="mr-2 h-4 w-4" />}
                Attach File
            </Button>
        </div>
    );
}
