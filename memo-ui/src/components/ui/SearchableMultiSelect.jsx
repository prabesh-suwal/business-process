import React, { useState, useRef, useEffect } from 'react';
import { Search, X, Check, ChevronDown } from 'lucide-react';

/**
 * SearchableMultiSelect - Premium searchable multi-select component
 * 
 * Features:
 * - Type to search/filter options
 * - Selected items shown as chips
 * - Keyboard navigation (up/down/enter/escape)
 * - Click outside to close
 * - Customizable display
 */
const SearchableMultiSelect = ({
    options = [],
    value: valueProp,
    selected,  // Alias for value
    onChange,
    placeholder = "Search...",
    label,
    valueKey = "value",  // Changed default from "code" to "value"
    labelKey = "label",  // Changed default from "name" to "label"
    icon: Icon,
    disabled = false,
    maxHeight = "200px"
}) => {
    // Support both 'value' and 'selected' props
    const value = valueProp ?? selected ?? [];

    const [isOpen, setIsOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [highlightedIndex, setHighlightedIndex] = useState(0);
    const containerRef = useRef(null);
    const inputRef = useRef(null);

    // Helper to safely get label as string
    const getLabel = (opt) => {
        if (!opt) return '';
        if (typeof opt === 'string') return opt;
        // Try the specified labelKey first
        const labelValue = opt[labelKey];
        if (typeof labelValue === 'string' && labelValue) return labelValue;
        // Try common fallbacks
        if (opt.displayName) return opt.displayName;
        if (opt.name) return opt.name;
        if (opt.fullName) return opt.fullName;
        if (opt.title) return opt.title;
        if (opt.label) return opt.label;
        if (opt.code) return opt.code;
        // Last resort: stringify
        if (labelValue && labelValue.toString) return labelValue.toString();
        return String(opt);
    };

    // Helper to safely get value - ALWAYS returns a string for use as React key
    const getValue = (opt) => {
        if (!opt) return '';
        if (typeof opt === 'string') return opt;
        const rawValue = opt[valueKey] || opt.value || opt.id || opt.code || opt.name;
        // Ensure we return a string for React keys
        if (rawValue === null || rawValue === undefined) return '';
        if (typeof rawValue === 'object') return JSON.stringify(rawValue);
        return String(rawValue);
    };

    // Filter options based on search
    const filteredOptions = options.filter(opt => {
        if (!search || search.trim() === '') return true; // Show all when no search
        const labelStr = getLabel(opt);
        return labelStr.toLowerCase().includes(search.toLowerCase());
    });

    // Close on click outside
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setIsOpen(false);
                setSearch('');
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Reset highlight when filtering
    useEffect(() => {
        setHighlightedIndex(0);
    }, [search]);

    const handleKeyDown = (e) => {
        if (!isOpen) {
            if (e.key === 'ArrowDown' || e.key === 'Enter') {
                setIsOpen(true);
                e.preventDefault();
            }
            return;
        }

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setHighlightedIndex(prev =>
                    prev < filteredOptions.length - 1 ? prev + 1 : prev
                );
                break;
            case 'ArrowUp':
                e.preventDefault();
                setHighlightedIndex(prev => prev > 0 ? prev - 1 : 0);
                break;
            case 'Enter':
                e.preventDefault();
                if (filteredOptions[highlightedIndex]) {
                    toggleOption(filteredOptions[highlightedIndex]);
                }
                break;
            case 'Escape':
                setIsOpen(false);
                setSearch('');
                break;
        }
    };

    const toggleOption = (option) => {
        const optValue = getValue(option);
        const isSelected = value.includes(optValue);

        if (isSelected) {
            onChange(value.filter(v => v !== optValue));
        } else {
            onChange([...value, optValue]);
        }
        setSearch('');
        inputRef.current?.focus();
    };

    const removeItem = (itemValue, e) => {
        e.stopPropagation();
        onChange(value.filter(v => v !== itemValue));
    };

    const getOptionLabel = (optValue) => {
        const option = options.find(o => getValue(o) === optValue);
        return option ? getLabel(option) : String(optValue);
    };

    return (
        <div ref={containerRef} className="relative">
            {label && (
                <label className="text-xs font-medium text-slate-600 mb-1.5 block flex items-center gap-1">
                    {Icon && <Icon className="w-3.5 h-3.5" />}
                    {label}
                </label>
            )}

            {/* Selected Items & Input */}
            <div
                className={`min-h-[42px] px-3 py-2 border rounded-lg bg-white flex flex-wrap items-center gap-1.5 cursor-text transition-all ${isOpen
                    ? 'border-blue-500 ring-2 ring-blue-500/20'
                    : 'border-slate-200 hover:border-slate-300'
                    } ${disabled ? 'opacity-50 cursor-not-allowed bg-slate-50' : ''}`}
                onClick={() => {
                    if (!disabled) {
                        setIsOpen(true);
                        inputRef.current?.focus();
                    }
                }}
            >
                {/* Selected Chips */}
                {value.map(v => (
                    <span
                        key={v}
                        className="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-md text-xs font-medium"
                    >
                        {getOptionLabel(v)}
                        {!disabled && (
                            <button
                                type="button"
                                onClick={(e) => removeItem(v, e)}
                                className="hover:text-blue-900 focus:outline-none"
                            >
                                <X className="w-3 h-3" />
                            </button>
                        )}
                    </span>
                ))}

                {/* Search Input */}
                <input
                    ref={inputRef}
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onFocus={() => setIsOpen(true)}
                    placeholder={value.length === 0 ? placeholder : ""}
                    disabled={disabled}
                    className="flex-1 min-w-[80px] bg-transparent border-0 outline-none text-sm placeholder:text-slate-400"
                />

                {/* Dropdown Icon */}
                <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </div>

            {/* Dropdown */}
            {isOpen && !disabled && (
                <div
                    className="absolute z-50 w-full mt-1 bg-white rounded-lg border border-slate-200 shadow-lg overflow-hidden"
                    style={{ maxHeight }}
                >
                    {/* Search hint */}
                    {search === '' && filteredOptions.length > 5 && (
                        <div className="px-3 py-2 bg-slate-50 border-b border-slate-100 text-xs text-slate-500 flex items-center gap-1">
                            <Search className="w-3 h-3" />
                            Type to search...
                        </div>
                    )}

                    {/* Options List */}
                    <div className="overflow-y-auto" style={{ maxHeight: `calc(${maxHeight} - 40px)` }}>
                        {filteredOptions.length === 0 ? (
                            <div className="px-3 py-4 text-center text-sm text-slate-500">
                                {search ? `No results for "${search}"` : 'No options available'}
                            </div>
                        ) : (
                            filteredOptions.map((option, index) => {
                                const optValue = getValue(option);
                                const optLabel = getLabel(option);
                                const isSelected = value.includes(optValue);
                                const isHighlighted = index === highlightedIndex;

                                return (
                                    <button
                                        key={optValue}
                                        type="button"
                                        onClick={() => toggleOption(option)}
                                        className={`w-full px-3 py-2.5 flex items-center gap-2 text-left text-sm transition-colors ${isHighlighted ? 'bg-blue-50' : 'hover:bg-slate-50'
                                            }`}
                                    >
                                        <div className={`w-4 h-4 rounded border flex items-center justify-center ${isSelected
                                            ? 'bg-blue-500 border-blue-500'
                                            : 'border-slate-300'
                                            }`}>
                                            {isSelected && <Check className="w-3 h-3 text-white" />}
                                        </div>
                                        <span className={isSelected ? 'font-medium text-slate-800' : 'text-slate-600'}>
                                            {optLabel}
                                        </span>
                                        {option.description && (
                                            <span className="text-xs text-slate-400 ml-auto">
                                                {option.description}
                                            </span>
                                        )}
                                    </button>
                                );
                            })
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default SearchableMultiSelect;
