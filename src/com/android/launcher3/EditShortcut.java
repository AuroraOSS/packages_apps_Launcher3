package com.android.launcher3;

import android.view.View;

import com.android.launcher3.popup.SystemShortcut;

public class EditShortcut extends SystemShortcut {

    public EditShortcut() {
        super(R.drawable.ic_edit, R.string.label_edit);
    }

    @Override
    public View.OnClickListener getOnClickListener(BaseDraggingActivity activity, ItemInfo itemInfo) {
        return new View.OnClickListener() {
            private boolean mOpened = false;

            @Override
            public void onClick(View view) {
                if (!mOpened) {
                    mOpened = true;
                    AbstractFloatingView.closeAllOpenViews(activity);
                    BottomSheet cbs = (BottomSheet) activity.getLayoutInflater()
                            .inflate(R.layout.edit_layout, activity.getDragLayer(), false);
                    cbs.populateAndShow(itemInfo);
                }
            }
        };
    }
}
