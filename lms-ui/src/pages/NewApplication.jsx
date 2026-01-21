import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { loanProducts, loanApplications } from '../api';

export default function NewApplication() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const productCode = searchParams.get('product');

    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        loanProductCode: productCode || '',
        applicantName: '',
        applicantEmail: '',
        applicantPhone: '',
        requestedAmount: '',
        requestedTenure: '',
        applicationData: {}
    });

    const [selectedProduct, setSelectedProduct] = useState(null);

    useEffect(() => {
        loadProducts();
    }, []);

    useEffect(() => {
        if (formData.loanProductCode && products.length > 0) {
            const prod = products.find(p => p.code === formData.loanProductCode);
            setSelectedProduct(prod);
        }
    }, [formData.loanProductCode, products]);

    const loadProducts = async () => {
        try {
            const data = await loanProducts.list();
            setProducts(data || []);
        } catch (err) {
            setError('Failed to load loan products');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSubmitting(true);

        try {
            // Create the application
            const application = await loanApplications.create({
                ...formData,
                requestedAmount: parseFloat(formData.requestedAmount),
                requestedTenure: parseInt(formData.requestedTenure),
            });

            // Submit it to start workflow
            await loanApplications.submit(application.id);

            navigate(`/applications/${application.id}`);
        } catch (err) {
            setError(err.message || 'Failed to submit application');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return <div className="main-content"><p>Loading...</p></div>;
    }

    return (
        <div className="main-content">
            <div className="page-header">
                <h1 className="page-title">New Loan Application</h1>
            </div>

            <div className="card">
                {error && (
                    <div style={{
                        background: '#fee2e2',
                        color: '#b91c1c',
                        padding: 'var(--spacing-3)',
                        borderRadius: 'var(--radius-md)',
                        marginBottom: 'var(--spacing-4)'
                    }}>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    {/* Loan Product Selection */}
                    <div className="form-group">
                        <label className="form-label">Loan Type *</label>
                        <select
                            name="loanProductCode"
                            value={formData.loanProductCode}
                            onChange={handleChange}
                            className="form-input"
                            required
                        >
                            <option value="">Select a loan type</option>
                            {products.map(p => (
                                <option key={p.code} value={p.code}>
                                    {p.name} - {p.interestRate}% p.a.
                                </option>
                            ))}
                        </select>
                    </div>

                    {selectedProduct && (
                        <div style={{
                            background: 'var(--color-primary-50)',
                            padding: 'var(--spacing-4)',
                            borderRadius: 'var(--radius-md)',
                            marginBottom: 'var(--spacing-4)'
                        }}>
                            <strong>{selectedProduct.name}</strong>
                            <p style={{ margin: 'var(--spacing-2) 0', color: 'var(--color-gray-600)' }}>
                                {selectedProduct.description}
                            </p>
                            <div style={{ display: 'flex', gap: 'var(--spacing-6)', fontSize: '0.875rem' }}>
                                <span>Amount: ₹{(selectedProduct.minAmount / 100000).toFixed(1)}L - ₹{(selectedProduct.maxAmount / 100000).toFixed(1)}L</span>
                                <span>Tenure: {selectedProduct.minTenure} - {selectedProduct.maxTenure} months</span>
                            </div>
                        </div>
                    )}

                    {/* Applicant Details */}
                    <h3 style={{ marginBottom: 'var(--spacing-4)' }}>Applicant Details</h3>

                    <div className="grid-2">
                        <div className="form-group">
                            <label className="form-label">Full Name *</label>
                            <input
                                type="text"
                                name="applicantName"
                                value={formData.applicantName}
                                onChange={handleChange}
                                className="form-input"
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Email *</label>
                            <input
                                type="email"
                                name="applicantEmail"
                                value={formData.applicantEmail}
                                onChange={handleChange}
                                className="form-input"
                                required
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Phone Number</label>
                        <input
                            type="tel"
                            name="applicantPhone"
                            value={formData.applicantPhone}
                            onChange={handleChange}
                            className="form-input"
                        />
                    </div>

                    {/* Loan Details */}
                    <h3 style={{ marginBottom: 'var(--spacing-4)', marginTop: 'var(--spacing-6)' }}>Loan Details</h3>

                    <div className="grid-2">
                        <div className="form-group">
                            <label className="form-label">Requested Amount (₹) *</label>
                            <input
                                type="number"
                                name="requestedAmount"
                                value={formData.requestedAmount}
                                onChange={handleChange}
                                className="form-input"
                                min={selectedProduct?.minAmount || 0}
                                max={selectedProduct?.maxAmount}
                                required
                            />
                            {selectedProduct && (
                                <small style={{ color: 'var(--color-gray-500)' }}>
                                    Min: ₹{selectedProduct.minAmount.toLocaleString()} | Max: ₹{selectedProduct.maxAmount.toLocaleString()}
                                </small>
                            )}
                        </div>
                        <div className="form-group">
                            <label className="form-label">Tenure (months) *</label>
                            <input
                                type="number"
                                name="requestedTenure"
                                value={formData.requestedTenure}
                                onChange={handleChange}
                                className="form-input"
                                min={selectedProduct?.minTenure || 1}
                                max={selectedProduct?.maxTenure}
                                required
                            />
                            {selectedProduct && (
                                <small style={{ color: 'var(--color-gray-500)' }}>
                                    Min: {selectedProduct.minTenure} months | Max: {selectedProduct.maxTenure} months
                                </small>
                            )}
                        </div>
                    </div>

                    {/* EMI Calculator Preview */}
                    {selectedProduct && formData.requestedAmount && formData.requestedTenure && (
                        <div style={{
                            background: 'var(--color-gray-50)',
                            padding: 'var(--spacing-4)',
                            borderRadius: 'var(--radius-md)',
                            marginBottom: 'var(--spacing-4)'
                        }}>
                            <strong>Estimated EMI</strong>
                            <p style={{
                                fontSize: '1.5rem',
                                fontWeight: '700',
                                color: 'var(--color-primary-600)',
                                margin: 'var(--spacing-2) 0'
                            }}>
                                ₹{calculateEMI(
                                    parseFloat(formData.requestedAmount),
                                    parseFloat(selectedProduct.interestRate),
                                    parseInt(formData.requestedTenure)
                                ).toLocaleString()}
                            </p>
                            <small style={{ color: 'var(--color-gray-500)' }}>
                                *This is an estimate. Actual EMI may vary.
                            </small>
                        </div>
                    )}

                    <div style={{ display: 'flex', gap: 'var(--spacing-3)', marginTop: 'var(--spacing-6)' }}>
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Submitting...' : 'Submit Application'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => navigate('/applications')}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function calculateEMI(principal, annualRate, tenureMonths) {
    const monthlyRate = annualRate / 12 / 100;
    if (monthlyRate === 0) return principal / tenureMonths;
    const emi = principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)
        / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
    return Math.round(emi);
}
