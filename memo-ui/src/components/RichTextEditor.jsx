import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Mention from '@tiptap/extension-mention';
import { Bold, Italic, Underline as UnderlineIcon, List, ListOrdered, Undo, Redo } from 'lucide-react';
import { Button } from './ui/button';
import { ReactRenderer } from '@tiptap/react';
import tippy from 'tippy.js';
import MentionList from './MentionList';
import { useEffect, useRef, useCallback } from 'react';

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

/**
 * Create the mention suggestion config for TipTap.
 * Uses tippy.js for positioning the dropdown.
 */
function createSuggestionConfig(usersRef) {
    return {
        items: ({ query }) => {
            const users = usersRef.current || [];
            return users
                .filter(item =>
                    item.label.toLowerCase().includes(query.toLowerCase())
                )
                .slice(0, 8);
        },

        render: () => {
            let component;
            let popup;

            return {
                onStart: (props) => {
                    component = new ReactRenderer(MentionList, {
                        props,
                        editor: props.editor,
                    });

                    if (!props.clientRect) return;

                    popup = tippy('body', {
                        getReferenceClientRect: props.clientRect,
                        appendTo: () => document.body,
                        content: component.element,
                        showOnCreate: true,
                        interactive: true,
                        trigger: 'manual',
                        placement: 'bottom-start',
                    });
                },

                onUpdate(props) {
                    component?.updateProps(props);

                    if (!props.clientRect) return;

                    popup?.[0]?.setProps({
                        getReferenceClientRect: props.clientRect,
                    });
                },

                onKeyDown(props) {
                    if (props.event.key === 'Escape') {
                        popup?.[0]?.hide();
                        return true;
                    }
                    return component?.ref?.onKeyDown(props);
                },

                onExit() {
                    popup?.[0]?.destroy();
                    component?.destroy();
                },
            };
        },
    };
}

export default function RichTextEditor({
    content,
    onChange,
    outputFormat = 'html',
    enableMentions = false,
    users = [],
    minHeight = '300px',
    placeholder = '',
    ...props
}) {
    const usersRef = useRef(users);

    useEffect(() => {
        usersRef.current = users;
    }, [users]);

    const extensions = [StarterKit, Underline];

    if (enableMentions) {
        extensions.push(
            Mention.configure({
                HTMLAttributes: {
                    class: 'mention',
                },
                suggestion: createSuggestionConfig(usersRef),
            })
        );
    }

    const editor = useEditor({
        extensions,
        content: content,
        onUpdate: ({ editor }) => {
            if (outputFormat === 'json') {
                onChange(editor.getJSON());
            } else {
                onChange(editor.getHTML());
            }
        },
        editorProps: {
            attributes: {
                class: `prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none p-4`,
                style: `min-height: ${minHeight}`,
            },
        },
    });

    return (
        <div className="border rounded-md bg-card">
            <MenuBar editor={editor} />
            <EditorContent editor={editor} />
            <style>{`
                .mention {
                    background-color: #dbeafe;
                    color: #1d4ed8;
                    border-radius: 4px;
                    padding: 1px 4px;
                    font-weight: 500;
                    font-size: 0.875rem;
                    box-decoration-break: clone;
                }
                .mention::before {
                    content: '@';
                }
                .ProseMirror ul {
                    list-style-type: disc;
                    padding-left: 1.25rem;
                }
                .ProseMirror ol {
                    list-style-type: decimal;
                    padding-left: 1.25rem;
                }
                .ProseMirror li {
                    margin: 2px 0;
                }
            `}</style>
        </div>
    );
}
