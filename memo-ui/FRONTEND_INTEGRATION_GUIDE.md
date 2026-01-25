# Frontend Integration Guide for View-Only Permissions

## Overview

This guide provides step-by-step instructions for integrating the view-only permissions UI into the existing application.

## Components Created

1. **`ViewerConfigPanel.jsx`** - Reusable component for configuring viewers
2. **`ViewOnlyMemos.jsx`** - Page to list view-only memos
3. **`ViewOnlyMemoDetail.jsx`** - Read-only memo detail view

## Step 1: Update API (Already Done)

Added to `src/lib/api.js`:
```javascript
getViewableMemos: () => api.get('/memo/api/memos/viewable').then(res => res.data),
canViewMemo: (id) => api.get(`/memo/api/memos/${id}/can-view`).then(res => res.data),
```

## Step 2: Add Routes

Update `src/App.jsx` or your router configuration:

```jsx
import ViewOnlyMemos from './pages/ViewOnlyMemos';
import ViewOnlyMemoDetail from './pages/ViewOnlyMemoDetail';

// Add these routes
<Route path="/view-only-memos" element={<ViewOnlyMemos />} />
<Route path="/memos/:id/view-only" element={<ViewOnlyMemoDetail />} />
```

## Step 3: Add Navigation Link

Update your sidebar navigation (e.g., `src/components/Sidebar.jsx`):

```jsx
import { Eye } from 'lucide-react';

// Add this menu item
{
  name: 'View-Only Memos',
  href: '/view-only-memos',
  icon: Eye
}
```

## Step 4: Integrate ViewerConfigPanel into WorkflowDesignerPage

Due to the complexity of `WorkflowDesignerPage.jsx` (839 lines), here's where and how to integrate the viewer configuration:

### Location 1: Memo-Wide Viewers (Top-Level)

Add a new section in the main workflow designer page, after the BPMN designer and before or alongside the step list:

```jsx
import ViewerConfigPanel from '../components/ViewerConfigPanel';

// Add state for memo-wide viewers
const [memoViewers, setMemoViewers] = useState([]);

// Load memo viewers in loadData()
if (topicData.viewerConfig && topicData.viewerConfig.viewers) {
  setMemoViewers(topicData.viewerConfig.viewers);
}

// Add UI section (around line 600-700, in the main layout)
<Card>
  <CardHeader>
    <CardTitle>Memo-Wide Viewers</CardTitle>
    <CardDescription>
      Users, roles, or departments that can view all memos and tasks for this topic
    </CardDescription>
  </CardHeader>
  <CardContent>
    <ViewerConfigPanel
      viewers={memoViewers}
      onChange={setMemoViewers}
      title="Who can view all memos?"
    />
  </CardContent>
</Card>
```

### Location 2: Step-Specific Viewers (Per Step)

Add viewer configuration alongside the existing step configuration (Who, Where, Conditions):

```jsx
// In the step configuration section (around line 700-800)
// Where you have the "Who", "Where", "Conditions" tabs, add a "Viewers" tab

<Tabs defaultValue="who">
  <TabsList>
    <TabsTrigger value="who">Who</TabsTrigger>
    <TabsTrigger value="where">Where</TabsTrigger>
    <TabsTrigger value="conditions">Conditions</TabsTrigger>
    <TabsTrigger value="viewers">Viewers</TabsTrigger> {/* NEW */}
  </TabsList>
  
  {/* Existing tabs... */}
  
  {/* NEW Viewers Tab */}
  <TabsContent value="viewers">
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
      title="Step Viewers"
    />
    <p className="text-xs text-gray-500 mt-2">
      These users can view tasks for this step only
    </p>
  </TabsContent>
</Tabs>
```

### Location 3: Update Save Logic

In `handleSaveAll()` method (around line 500-600), update to include viewer config:

```jsx
const handleSaveAll = async () => {
  // ... existing save logic ...
  
  // Save memo-wide viewers
  if (memoViewers.length > 0) {
    await MemoApi.updateTopicViewers(topicId, { viewers: memoViewers });
  }
  
  // Save step-specific viewers in step configs
  for (const step of allSteps) {
    const config = stepConfigs[step.id];
    if (config && config.viewers) {
      // Include viewers in the step config save
      const configPayload = {
        // ... existing fields ...
        viewerConfig: { viewers: config.viewers }
      };
      await WorkflowConfigApi.saveStepConfig(topicId, step.id, configPayload);
    }
  }
};
```

## Step 5: Add Backend Endpoint for Updating Viewers

You'll need to add this endpoint to `MemoConfigurationController.java`:

```java
@PatchMapping("/topics/{id}/viewers")
public ResponseEntity<Void> updateTopicViewers(
        @PathVariable UUID id,
        @RequestBody ViewerConfigDTO viewerConfig) {
    memoConfigurationService.updateTopicViewers(id, viewerConfig);
    return ResponseEntity.ok().build();
}
```

And corresponding service method in `MemoConfigurationService.java`:

```java
public void updateTopicViewers(UUID topicId, ViewerConfigDTO viewerConfig) {
    MemoTopic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new RuntimeException("Topic not found"));
    
    // Convert DTO to Map for JSON storage
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("viewers", viewerConfig.getViewers());
    topic.setViewerConfig(configMap);
    
    topicRepository.save(topic);
}
```

## Step 6: Test the Integration

1. **Start the backend services** (memo-service with the new migration)
2. **Navigate to Workflow Designer** for a topic
3. **Add memo-wide viewers** (e.g., Role: MMS_VIEWER)
4. **Add step-specific viewers** for specific steps
5. **Save & Deploy** the workflow
6. **Login as a viewer user** and check:
   - "View-Only Memos" appears in sidebar
   - Memos are visible in the view-only list
   - Click to view shows read-only detail
   - No action buttons are shown

## Summary

The integration requires:
1. ✅ API methods added
2. ⏳ Routes to be added
3. ⏳ Navigation link to be added
4. ⏳ ViewerConfigPanel integrated into WorkflowDesignerPage
5. ⏳ Backend update endpoint to be added
6. ⏳ Testing

The heavy lifting is done - the components are ready. The integration points are clearly marked above!
