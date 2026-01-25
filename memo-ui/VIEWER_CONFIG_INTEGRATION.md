# Quick Integration Summary - View-Only Permissions in Workflow Designer

## What's Already Done ✅

1. **Component Ready**: `ViewerConfigPanel.jsx` - fully functional
2. **State Added**: `memoWideViewers` state for memo-wide configuration
3. **Import Added**: ViewerConfigPanel imported into WorkflowDesignerPage

## Integration Approach

Due to the complexity of WorkflowDesignerPage.jsx (840+ lines), I'm providing you with two clear sections to add:

### 1. Memo-Wide Viewer Configuration (Top-Level Card)

**Location**: Add after line ~480, before the "Step List" section

**Code to Add**:
```jsx
{/* Memo-Wide Viewer Configuration */}
<Card className="mt-6">
    <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 border-b">
        <div className="flex items-center gap-2">
            <Eye className="h-5 w-5 text-blue-600" />
            <div>
                <CardTitle>Memo-Wide Viewers</CardTitle>
                <CardDescription>
                    Configure who can view ALL memos and tasks for this workflow (read-only access)
                </CardDescription>
            </div>
        </div>
    </CardHeader>
    <CardContent className="pt-6">
        <ViewerConfigPanel
            viewers={memoWideViewers}
            onChange={setMemoWideViewers}
            title="Who can view all memos in this workflow?"
        />
        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-xs text-blue-800">
                <strong>💡 Tip:</strong> These viewers will have read-only access to view all memos created under this topic, including all workflow steps and decisions.
            </p>
        </div>
    </CardContent>
</Card>
```

### 2. Step-Specific Viewer Configuration (In Step Config Tabs)

**Location**: Inside the step configuration tabs (around line ~720), add a new tab after "Conditions"

**Code to Add**:
```jsx
{/* Add to TabsList */}
<TabsTrigger value="viewers" className="gap-2">
    <Eye className="h-4 w-4" />
    Viewers
</TabsTrigger>

{/* Add to TabsContent sections */}
<TabsContent value="viewers" className="space-y-4">
    <ViewerConfigPanel
        viewers={stepConfigs[selectedStep?.id]?.viewers || []}
        onChange={(viewers) => {
            setStepConfigs({
                ...stepConfigs,
                [selectedStep.id]: {
                    ...stepConfigs[selectedStep.id],
                    viewers
                }
            });
        }}
        title="Who can view tasks for this step?"
    />
    <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
        <p className="text-xs text-amber-800">
            <strong>⚠️ Note:</strong> These viewers can only see tasks for THIS specific step. They won't see other steps unless configured separately or given memo-wide access.
        </p>
    </div>
</TabsContent>
```

### 3. Load Viewers from Topic Data

**Location**: In `loadData()` function (around line 60-90)

**Code to Add**:
```jsx
// After setting topic data
if (topicData.viewerConfig && topicData.viewerConfig.viewers) {
    setMemoWideViewers(topicData.viewerConfig.viewers);
}

// In step configs loading (update the forEach loop around line 76)
configMap[c.taskKey] = {
    assignmentType: c.assignmentConfig?.type,
    role: c.assignmentConfig?.role,
    scope: c.assignmentConfig?.scope,
    group: c.assignmentConfig?.groupCode,
    duration: c.slaConfig?.duration,
    escalationAction: c.escalationConfig?.escalations?.[0]?.action,
    viewers: c.viewerConfig?.viewers || []  // ADD THIS LINE
};
```

### 4. Save Viewers in Deploy Function

**Location**: In `handleDeploy()` or save function (search for "deploy" around line 400-500)

**Code to Add**:
```jsx
// Save memo-wide viewers
if (memoWideViewers && memoWideViewers.length > 0) {
    try {
        await MemoApi.updateTopicViewers(topicId, { viewers: memoWideViewers });
        console.log('Memo-wide viewers saved');
    } catch (err) {
        console.error('Failed to save memo-wide viewers:', err);
    }
}

// Step-specific viewers are saved with step configs (they're already in stepConfigs)
```

### 5. Add API Method

**Location**: `/memo-ui/src/lib/api.js` in MemoApi object

**Code to Add**:
```javascript
// In MemoApi object
updateTopicViewers: (topicId, viewerConfig) => 
    api.patch(`/memo-config/topics/${topicId}/viewers`, viewerConfig).then(res => res.data),
```

## User Experience Flow

1. **User opens Workflow Designer** for a topic
2. **Top section shows "Memo-Wide Viewers"** card - configure once for entire workflow
3. **Click a BPMN step** → Right panel shows step configuration tabs
4. **New "Viewers" tab** → Configure step-specific viewers
5. **Click "Save & Deploy"** → Everything saves together

## Visual Hierarchy

```
┌─────────────────────────────────────────┐
│  Workflow XML / BPMN Designer (Left)    │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📋 Memo-Wide Viewers (Card)            │
│  └─ Who can view ALL memos?             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  📊 Step List (Existing)                │
└─────────────────────────────────────────┘

[When step selected] →

┌─────────────────────────────────────────┐
│  Step Configuration (Right Panel)       │
│  ├─ Tab: Who (assign tasks)             │
│  ├─ Tab: Where (location/dept)          │
│  ├─ Tab: Conditions                     │
│  └─ Tab: 👁️ Viewers (NEW!)             │
│     └─ Who can view THIS step?          │
└─────────────────────────────────────────┘
```

## Next Steps

You can either:
1. **Let me complete the integration** by adding these exact code snippets
2. **Do it yourself** using this guide
3. **Test first** with database-configured viewers before UI integration

Which would you prefer?
