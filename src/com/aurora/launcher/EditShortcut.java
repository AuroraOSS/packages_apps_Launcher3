package com.aurora.launcher;

import android.view.View;

import com.aurora.launcher.popup.SystemShortcut;

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
                    EditBottomSheet cbs = (EditBottomSheet) activity.getLayoutInflater()
                            .inflate(R.layout.edit_layout, activity.getDragLayer(), false);
                    cbs.populateAndShow(itemInfo);
                }
            }
        };
    }
}
