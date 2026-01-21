import React from 'react';

export default function DataTable({
    columns,
    data,
    keyField = 'id',
    loading = false,
    emptyMessage = "No data found",
    onRowClick
}) {
    if (loading) {
        return <div className="loading">Loading...</div>;
    }

    if (!data || data.length === 0) {
        return <div className="empty-state">{emptyMessage}</div>;
    }

    return (
        <div className="table-container">
            <table>
                <thead>
                    <tr>
                        {columns.map((col, index) => (
                            <th key={index} style={{ width: col.width }}>
                                {col.header}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {data.map((row) => (
                        <tr
                            key={row[keyField]}
                            onClick={() => onRowClick && onRowClick(row)}
                            className={onRowClick ? 'clickable-row' : ''}
                            style={{ cursor: onRowClick ? 'pointer' : 'default' }}
                        >
                            {columns.map((col, index) => (
                                <td key={index}>
                                    {col.render ? col.render(row) : row[col.accessor]}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
