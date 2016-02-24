/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.sample.remote_scripting;

import java.util.ArrayList;
import java.util.List;

import org.gearvrf.GVRActivity;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class GearVRAndroidUI {

    private static final List<String> items = new ArrayList<String>(5);
    private GVRFrameLayout frameLayout;
    private ListView listView;
    private TextView listTextView;
    private String listItemClicked;
    private GearVRScripting mActivity;

    static {
        items.add("background1.jpg");
        items.add("background2.jpg");
        items.add("background3.jpg");
        items.add("background4.jpg");
        items.add("background5.jpg");
    }

    public GearVRAndroidUI(GVRActivity activity) {
        mActivity = (GearVRScripting) activity;
        frameLayout = new GVRFrameLayout(activity);
        frameLayout.setBackgroundColor(Color.WHITE);
        View.inflate(activity, R.layout.activity_main, frameLayout);
        listTextView = (TextView) frameLayout.findViewById(R.id.listTextView);
        listView = (ListView) frameLayout.findViewById(R.id.listView);
        listView.setBackgroundColor(Color.LTGRAY);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);
        listItemClicked = activity.getResources().getString(R.string.listClicked);
    }

    public GVRFrameLayout getFrameLayout() {
        return frameLayout;
    }

    private OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
            //listTextView.setText(String.format("%s %s", listItemClicked, items.get(position)));
            mActivity.setNewBackground(items.get(position));
        }
    };

}
