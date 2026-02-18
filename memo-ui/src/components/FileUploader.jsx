import React, { useState, useRef } from 'react';
import { Button } from './ui/button';
import { Paperclip, Loader2 } from 'lucide-react';
import { MemoApi } from '../lib/api';
import { toast } from 'sonner';

export default function FileUploader({ memoId, onUploadComplete, customTrigger }) {
    const [uploading, setUploading] = useState(false);
    const fileInputRef = useRef(null);

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setUploading(true);
        try {
            await MemoApi.uploadAttachment(memoId, file);
            toast.success(`Uploaded: ${file.name}`);
            onUploadComplete();
        } catch (error) {
            console.error("Upload failed", error);
            toast.error("Upload failed");
        } finally {
            setUploading(false);
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    return (
        <>
            <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                onChange={handleFileChange}
            />
            {customTrigger ? (
                <span onClick={() => !uploading && fileInputRef.current?.click()} className="inline-block">
                    {uploading ? <Loader2 className="h-4 w-4 animate-spin inline" /> : customTrigger}
                </span>
            ) : (
                <Button
                    variant="outline"
                    size="sm"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                >
                    {uploading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Paperclip className="mr-2 h-4 w-4" />}
                    Attach File
                </Button>
            )}
        </>
    );
}
