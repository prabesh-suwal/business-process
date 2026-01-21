import React, { useEffect, useState } from 'react';
import { MemoApi } from '../lib/api';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { ChevronRight } from 'lucide-react';

export default function CreateMemo() {
    const navigate = useNavigate();
    const [categories, setCategories] = useState([]);
    const [topics, setTopics] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [selectedTopic, setSelectedTopic] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        MemoApi.getCategories()
            .then(setCategories)
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    const handleCategorySelect = (category) => {
        setSelectedCategory(category);
        setSelectedTopic(null);
        setLoading(true);
        MemoApi.getTopics(category.id)
            .then(setTopics)
            .catch(console.error)
            .finally(() => setLoading(false));
    };

    const handleCreateDraft = () => {
        if (!selectedTopic) return;

        MemoApi.createDraft({
            topicId: selectedTopic.id,
            subject: `Memo: ${selectedTopic.name}`, // Default subject
            priority: 'NORMAL'
        })
            .then((memo) => {
                navigate(`/edit/${memo.id}`);
            })
            .catch(console.error);
    };

    if (loading && !selectedCategory) {
        return <div className="p-8">Loading configuration...</div>;
    }

    return (
        <div className="max-w-4xl mx-auto space-y-8">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Create New Memo</h1>
                <p className="text-muted-foreground mt-2">Select a category and topic to begin.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Categories */}
                <div className="space-y-4">
                    <h2 className="text-lg font-semibold">1. Select Category</h2>
                    <div className="space-y-2">
                        {categories.map((cat) => (
                            <div
                                key={cat.id}
                                onClick={() => handleCategorySelect(cat)}
                                className={`p-4 rounded-lg border cursor-pointer transition-all hover:bg-muted/50
                                    ${selectedCategory?.id === cat.id ? 'border-primary ring-1 ring-primary bg-primary/5' : 'bg-card'}`}
                            >
                                <div className="font-medium">{cat.name}</div>
                                <div className="text-sm text-muted-foreground">{cat.description}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Topics */}
                {selectedCategory && (
                    <div className="space-y-4 animate-in fade-in slide-in-from-left-4">
                        <h2 className="text-lg font-semibold">2. Select Topic</h2>
                        <div className="space-y-2">
                            {topics.map((topic) => (
                                <div
                                    key={topic.id}
                                    onClick={() => setSelectedTopic(topic)}
                                    className={`p-4 rounded-lg border cursor-pointer transition-all hover:bg-muted/50
                                        ${selectedTopic?.id === topic.id ? 'border-primary ring-1 ring-primary bg-primary/5' : 'bg-card'}`}
                                >
                                    <div className="font-medium">{topic.name}</div>
                                    <div className="text-sm text-muted-foreground">{topic.description}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            <div className="flex justify-end pt-8 border-t">
                <Button
                    size="lg"
                    disabled={!selectedTopic}
                    onClick={handleCreateDraft}
                >
                    Create Draft <ChevronRight className="ml-2 h-4 w-4" />
                </Button>
            </div>
        </div>
    );
}
