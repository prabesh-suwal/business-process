import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import { Bold, Italic, Underline as UnderlineIcon, List, ListOrdered, Undo, Redo } from 'lucide-react';
import { Button } from './ui/button';

const MenuBar = ({ editor }) => {
    if (!editor) {
        return null;
    }

    return (
        <div className="flex flex-wrap gap-1 p-2 border-b bg-muted/30">
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().toggleBold().run()}
                className={editor.isActive('bold') ? 'bg-muted' : ''}
            >
                <Bold className="h-4 w-4" />
            </Button>
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().toggleItalic().run()}
                className={editor.isActive('italic') ? 'bg-muted' : ''}
            >
                <Italic className="h-4 w-4" />
            </Button>
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().toggleUnderline().run()}
                className={editor.isActive('underline') ? 'bg-muted' : ''}
            >
                <UnderlineIcon className="h-4 w-4" />
            </Button>
            <div className="w-px h-6 bg-border mx-1" />
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().toggleBulletList().run()}
                className={editor.isActive('bulletList') ? 'bg-muted' : ''}
            >
                <List className="h-4 w-4" />
            </Button>
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().toggleOrderedList().run()}
                className={editor.isActive('orderedList') ? 'bg-muted' : ''}
            >
                <ListOrdered className="h-4 w-4" />
            </Button>
            <div className="w-px h-6 bg-border mx-1" />
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().undo().run()}
                disabled={!editor.can().undo()}
            >
                <Undo className="h-4 w-4" />
            </Button>
            <Button
                variant="ghost"
                size="sm"
                onClick={() => editor.chain().focus().redo().run()}
                disabled={!editor.can().redo()}
            >
                <Redo className="h-4 w-4" />
            </Button>
        </div>
    );
};

export default function RichTextEditor({ content, onChange }) {
    const editor = useEditor({
        extensions: [
            StarterKit,
            Underline,
        ],
        content: content,
        onUpdate: ({ editor }) => {
            onChange(editor.getJSON());
        },
        editorProps: {
            attributes: {
                class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none min-h-[300px] p-4',
            },
        },
    });

    return (
        <div className="border rounded-md bg-card">
            <MenuBar editor={editor} />
            <EditorContent editor={editor} />
        </div>
    );
}
