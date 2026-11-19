/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.model.data.ItemInfo;

/**
 * Drop target shown only while a multi-selection of home screen apps is being dragged. Dropping
 * the selection here asks for a name and groups the selected apps into a new folder.
 */
public class BatchCreateFolderDropTarget extends ButtonDropTarget {

    private static final int NO_ACCESSIBILITY_ACTION = -1;

    public BatchCreateFolderDropTarget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        super.onDragStart(dragObject, options);
        // The base class hides every target for batch drags; this one is only for batch drags.
        if (options.isWorkspaceBatchDrag) {
            mActive = true;
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void setupItemInfo(ItemInfo info) {
        // Nothing to set up: the label is fixed.
    }

    @Override
    protected boolean supportsDrop(ItemInfo info) {
        return Launcher.getLauncher(getContext()).getWorkspace().getBatchEditor().isDragging();
    }

    @Override
    public int getSupportedAccessibilityAction(ItemInfo info, View view) {
        return NO_ACCESSIBILITY_ACTION;
    }

    @Override
    public int getAccessibilityAction() {
        return NO_ACCESSIBILITY_ACTION;
    }

    @Override
    public void onAccessibilityDrop(View view, ItemInfo item, int action) {
        // Batch drops are only reachable by touch.
    }

    @Override
    public void completeDrop(DropTarget.DragObject d) {
        Launcher.getLauncher(getContext()).getWorkspace().getBatchEditor().onCreateFolderDrop();
    }
}
