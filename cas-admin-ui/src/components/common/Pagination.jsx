import React from 'react';

export default function Pagination({
    currentPage,
    totalPages,
    onPageChange,
    totalItems,
    itemsPerPage,
    className = ""
}) {
    if (totalPages <= 1 && !totalItems) return null;

    // Calculate range (e.g., Showing 1-10 of 50)
    const startItem = currentPage * itemsPerPage + 1;
    const endItem = Math.min((currentPage + 1) * itemsPerPage, totalItems);

    return (
        <div className={`pagination ${className}`}>
            <span className="pagination-info">
                Showing <strong>{startItem}</strong> to <strong>{endItem}</strong> of <strong>{totalItems}</strong> results
            </span>
            <div className="pagination-buttons">
                <button
                    className="btn btn-sm btn-secondary"
                    disabled={currentPage === 0}
                    onClick={() => onPageChange(currentPage - 1)}
                >
                    Previous
                </button>
                <div className="pagination-numbers">
                    <span className="current-page">Page {currentPage + 1} of {totalPages}</span>
                </div>
                <button
                    className="btn btn-sm btn-secondary"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => onPageChange(currentPage + 1)}
                >
                    Next
                </button>
            </div>
        </div>
    );
}
