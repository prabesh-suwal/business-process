import React from 'react';

export default function SearchInput({ value, onChange, placeholder = "Search...", className = "" }) {
    return (
        <div className={`search-box ${className}`}>
            <div className="search-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
            </div>
            <input
                type="text"
                className="form-input search-input-field"
                value={value}
                onChange={(e) => onChange(e.target.value)}
                placeholder={placeholder}
            />
        </div>
    );
}
