import React, { forwardRef, useEffect, useImperativeHandle, useState } from 'react';

/**
 * Mention suggestion dropdown for TipTap editor.
 * Shows a filterable list of users when @ is typed.
 */
const MentionList = forwardRef((props, ref) => {
    const [selectedIndex, setSelectedIndex] = useState(0);

    const selectItem = (index) => {
        const item = props.items[index];
        if (item) {
            props.command({ id: item.id, label: item.label });
        }
    };

    const upHandler = () => {
        setSelectedIndex((selectedIndex + props.items.length - 1) % props.items.length);
    };

    const downHandler = () => {
        setSelectedIndex((selectedIndex + 1) % props.items.length);
    };

    const enterHandler = () => {
        selectItem(selectedIndex);
    };

    useEffect(() => setSelectedIndex(0), [props.items]);

    useImperativeHandle(ref, () => ({
        onKeyDown: ({ event }) => {
            if (event.key === 'ArrowUp') {
                upHandler();
                return true;
            }
            if (event.key === 'ArrowDown') {
                downHandler();
                return true;
            }
            if (event.key === 'Enter') {
                enterHandler();
                return true;
            }
            return false;
        },
    }));

    if (!props.items.length) {
        return (
            <div className="bg-white border border-slate-200 rounded-lg shadow-lg p-3 text-sm text-slate-400">
                No users found
            </div>
        );
    }

    return (
        <div className="bg-white border border-slate-200 rounded-lg shadow-lg overflow-hidden min-w-[200px] max-h-[280px] overflow-y-auto">
            {props.items.map((item, index) => (
                <button
                    key={item.id}
                    onClick={() => selectItem(index)}
                    className={`
                        flex items-center gap-3 w-full px-3 py-2.5 text-left text-sm transition-colors
                        ${index === selectedIndex
                            ? 'bg-blue-50 text-blue-700'
                            : 'text-slate-700 hover:bg-slate-50'
                        }
                    `}
                >
                    <div className="flex-shrink-0 h-7 w-7 rounded-full bg-slate-800 text-white flex items-center justify-center text-xs font-medium">
                        {item.label.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()}
                    </div>
                    <div className="flex flex-col">
                        <span className="font-medium text-sm">{item.label}</span>
                        {item.role && (
                            <span className="text-xs text-slate-400">{item.role}</span>
                        )}
                    </div>
                </button>
            ))}
        </div>
    );
});

MentionList.displayName = 'MentionList';

export default MentionList;
