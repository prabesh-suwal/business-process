import React from 'react';

export default function FilterSelect({ value, onChange, options, label, placeholder = "All", className = "" }) {
    return (
        <div className={`filter-select ${className}`}>
            {label && <label className="filter-label">{label}</label>}
            <select
                className="form-select"
                value={value}
                onChange={(e) => onChange(e.target.value)}
            >
                <option value="">{placeholder}</option>
                {options.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                        {opt.label}
                    </option>
                ))}
            </select>
        </div>
    );
}
