import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { loanProducts } from '../api';

export default function LoanProducts() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadProducts();
    }, []);

    const loadProducts = async () => {
        try {
            const data = await loanProducts.list();
            setProducts(data || []);
        } catch (error) {
            console.error('Failed to load products:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="main-content"><p>Loading...</p></div>;
    }

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">Loan Products</h1>
            </div>

            <div className="grid-3">
                {products.map((product) => (
                    <div key={product.id} className="card">
                        <div className="card-header">
                            <h3 className="card-title">{product.name}</h3>
                            <span className={`badge ${product.active ? 'badge-approved' : 'badge-draft'}`}>
                                {product.active ? 'Active' : 'Inactive'}
                            </span>
                        </div>

                        <p style={{ color: 'var(--color-gray-600)', marginBottom: 'var(--spacing-4)' }}>
                            {product.description || 'No description'}
                        </p>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-2)', marginBottom: 'var(--spacing-4)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: 'var(--color-gray-500)' }}>Interest Rate</span>
                                <strong>{product.interestRate}% p.a.</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: 'var(--color-gray-500)' }}>Amount Range</span>
                                <strong>₹{(product.minAmount / 100000).toFixed(1)}L - ₹{(product.maxAmount / 100000).toFixed(1)}L</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: 'var(--color-gray-500)' }}>Tenure</span>
                                <strong>{product.minTenure} - {product.maxTenure} months</strong>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span style={{ color: 'var(--color-gray-500)' }}>Processing Fee</span>
                                <strong>{product.processingFeePercent || 0}%</strong>
                            </div>
                        </div>

                        <Link
                            to={`/applications/new?product=${product.code}`}
                            className="btn btn-primary"
                            style={{ width: '100%' }}
                        >
                            Apply Now
                        </Link>
                    </div>
                ))}
            </div>

            {products.length === 0 && (
                <div className="card">
                    <p style={{ textAlign: 'center', color: 'var(--color-gray-500)' }}>
                        No loan products available
                    </p>
                </div>
            )}
        </div>
    );
}
