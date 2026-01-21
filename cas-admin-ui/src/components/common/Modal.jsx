import { useEffect, useRef } from 'react';

export default function Modal({ isOpen, onClose, title, children, size = 'medium' }) {
    const modalRef = useRef(null);

    useEffect(() => {
        const handleEscape = (e) => {
            if (e.key === 'Escape') onClose();
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = '';
        };
    }, [isOpen, onClose]);

    const handleBackdropClick = (e) => {
        if (e.target === modalRef.current) {
            onClose();
        }
    };

    if (!isOpen) return null;

    const widthMap = {
        small: '400px',
        medium: '600px',
        large: '900px',
        xlarge: '1200px'
    };

    return (
        <div
            ref={modalRef}
            onClick={handleBackdropClick}
            style={{
                position: 'fixed',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                background: 'rgba(0, 0, 0, 0.6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 1000,
                backdropFilter: 'blur(4px)'
            }}
        >
            <div
                style={{
                    background: 'var(--bg-primary)',
                    borderRadius: '12px',
                    width: widthMap[size] || widthMap.medium,
                    maxWidth: '90vw',
                    maxHeight: '90vh',
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)'
                }}
            >
                {/* Header */}
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '16px 20px',
                        borderBottom: '1px solid var(--border-color)'
                    }}
                >
                    <h2 style={{ margin: 0, fontSize: '18px' }}>{title}</h2>
                    <button
                        onClick={onClose}
                        style={{
                            background: 'none',
                            border: 'none',
                            fontSize: '24px',
                            cursor: 'pointer',
                            color: 'var(--text-secondary)',
                            padding: '0 8px',
                            lineHeight: 1
                        }}
                    >
                        ×
                    </button>
                </div>

                {/* Content */}
                <div
                    style={{
                        padding: '20px',
                        overflowY: 'auto',
                        flex: 1
                    }}
                >
                    {children}
                </div>
            </div>
        </div>
    );
}
