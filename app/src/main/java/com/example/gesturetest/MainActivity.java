// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.gesturetest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Main activity of MediaPipe hand tracking app.
 */
public class MainActivity extends BasicActivity {
    private static final String TAG = "MainActivity";

    private static final String INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    // Max number of hands to detect/process.
    private static final int NUM_HANDS = 1;
    private static final int NUM_FRAMES = 5;
    
    private TextView mTextView;
    private View mViewUp, mViewRight, mViewDown, mViewLeft;
    private final LinkedList<FrameData> listFrame = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextView = findViewById(R.id.tips);
        mViewUp = findViewById(R.id.up);
        mViewRight = findViewById(R.id.right);
        mViewDown = findViewById(R.id.down);
        mViewLeft = findViewById(R.id.left);
        refreshDir(-1);

        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
        // if (Log.isLoggable(TAG, Log.VERBOSE)) {
        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
//            Log.e(TAG, "Received multi-hand landmarks packet.");
                    List<NormalizedLandmarkList> multiHandLandmarks =
                            PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());
//            Log.e(
//                TAG,
//                "[TS:"
//                    + packet.getTimestamp()
//                    + "] "
//                    + getMultiHandLandmarksDebugString(multiHandLandmarks));
                    FrameData frame = new FrameData();
                    listFrame.add(frame);                    
                    if (multiHandLandmarks.get(0).getLandmarkCount() < 21) {
                        if (listFrame.size() > NUM_FRAMES) {
                            listFrame.poll();
                        }
                        return;
                    }
                    NormalizedLandmark landmark6 = multiHandLandmarks.get(0).getLandmark(6);
                    NormalizedLandmark landmark7 = multiHandLandmarks.get(0).getLandmark(7);
                    NormalizedLandmark landmark8 = multiHandLandmarks.get(0).getLandmark(8);
                    NormalizedLandmark landmark10 = multiHandLandmarks.get(0).getLandmark(10);
                    NormalizedLandmark landmark11 = multiHandLandmarks.get(0).getLandmark(11);
                    NormalizedLandmark landmark12 = multiHandLandmarks.get(0).getLandmark(12);
                    NormalizedLandmark landmark14 = multiHandLandmarks.get(0).getLandmark(14);
                    NormalizedLandmark landmark15 = multiHandLandmarks.get(0).getLandmark(15);
                    NormalizedLandmark landmark16 = multiHandLandmarks.get(0).getLandmark(16);
                    NormalizedLandmark landmark18 = multiHandLandmarks.get(0).getLandmark(18);
                    NormalizedLandmark landmark19 = multiHandLandmarks.get(0).getLandmark(19);
                    NormalizedLandmark landmark20 = multiHandLandmarks.get(0).getLandmark(20);
                    if (!(landmark8.getY() < landmark7.getY() &&
                            landmark12.getY() < landmark11.getY() &&
                            landmark16.getY() < landmark15.getY() &&
                            landmark20.getY() < landmark19.getY())) {
                        if (listFrame.size() > NUM_FRAMES) {
                            listFrame.poll();
                        }
                        return;
                    }

                    frame.isValid = true;
                    NormalizedLandmark landmark = multiHandLandmarks.get(0).getLandmark(8);
                    float x = landmark.getX();
                    float y = landmark.getY();
//                    Log.e(TAG, "x = " + x + " y = " + y);
                    frame.x = x;
                    frame.y = y;
                    if (listFrame.size() <= NUM_FRAMES) {
                        return;
                    }
                    float sumx = 0, sumy = 0;
                    int count = 0;
                    for (int i = 0; i < NUM_FRAMES; ++i) {
                        FrameData f = listFrame.get(i);
                        if (listFrame.get(i).isValid) {
                            sumx += f.x;
                            sumy += f.y;
                            ++count;
                        }
                    }
                    if (count < 3) {
                        listFrame.poll();
                        return;
                    }
                    sumx /= count;
                    sumy /= count;
//                    Log.e(TAG, "sumx = " + sumx + " sumy = " + sumy);
                    float difx = sumx - x;
                    float dify = sumy - y;
                    String tips = "";
                    if (Math.abs(difx) >= Math.abs(dify)) {
                        if (difx > 0.12) {
                            tips = "左";
                            runOnUiThread(() -> refreshDir(3));
                        } else if (difx < -0.12) {
                            tips = "右";
                            runOnUiThread(() -> refreshDir(1));
                        }
                    } else {
                        if (dify > 0.12) {
                            tips = "上";
                            runOnUiThread(() -> refreshDir(0));
                        } else if (dify < -0.12) {
                            tips = "下";
                            runOnUiThread(() -> refreshDir(2));
                        }
                    }
                    String finalStr = tips;
                    runOnUiThread(() -> mTextView.setText(finalStr));
                    Log.e(TAG, tips);
                    listFrame.poll();
//                    Log.e(TAG, "list size " + listFrame.size());
                });
        // }
    }

    private String getMultiHandLandmarksDebugString(List<NormalizedLandmarkList> multiHandLandmarks) {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        StringBuilder multiHandLandmarksStr = new StringBuilder("Number of hands detected: " + multiHandLandmarks.size() + "\n");
        int handIndex = 0;
        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr.append("\t#Hand landmarks for hand[").append(handIndex).append("]: ").append(landmarks.getLandmarkCount()).append("\n");
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr.append("\t\tLandmark [").append(landmarkIndex).append("]: (").append(landmark.getX()).append(", ").append(landmark.getY()).append(", ").append(landmark.getZ()).append(")\n");
                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr.toString();
    }

    private void refreshDir(int dir) {
        switch (dir) {
            case 0:
                if (mViewUp.getVisibility() == View.INVISIBLE)
                    mViewUp.setVisibility(View.VISIBLE);
                if (mViewRight.getVisibility() == View.VISIBLE)
                    mViewRight.setVisibility(View.INVISIBLE);
                if (mViewDown.getVisibility() == View.VISIBLE)
                    mViewDown.setVisibility(View.INVISIBLE);
                if (mViewLeft.getVisibility() == View.VISIBLE)
                    mViewLeft.setVisibility(View.INVISIBLE);
                break;
            case 1:
                if (mViewUp.getVisibility() == View.VISIBLE)
                    mViewUp.setVisibility(View.INVISIBLE);
                if (mViewRight.getVisibility() == View.INVISIBLE)
                    mViewRight.setVisibility(View.VISIBLE);
                if (mViewDown.getVisibility() == View.VISIBLE)
                    mViewDown.setVisibility(View.INVISIBLE);
                if (mViewLeft.getVisibility() == View.VISIBLE)
                    mViewLeft.setVisibility(View.INVISIBLE);
                break;
            case 2:
                if (mViewUp.getVisibility() == View.VISIBLE)
                    mViewUp.setVisibility(View.INVISIBLE);
                if (mViewRight.getVisibility() == View.VISIBLE)
                    mViewRight.setVisibility(View.INVISIBLE);
                if (mViewDown.getVisibility() == View.INVISIBLE)
                    mViewDown.setVisibility(View.VISIBLE);
                if (mViewLeft.getVisibility() == View.VISIBLE)
                    mViewLeft.setVisibility(View.INVISIBLE);
                break;
            case 3:
                if (mViewUp.getVisibility() == View.VISIBLE)
                    mViewUp.setVisibility(View.INVISIBLE);
                if (mViewRight.getVisibility() == View.VISIBLE)
                    mViewRight.setVisibility(View.INVISIBLE);
                if (mViewDown.getVisibility() == View.VISIBLE)
                    mViewDown.setVisibility(View.INVISIBLE);
                if (mViewLeft.getVisibility() == View.INVISIBLE)
                    mViewLeft.setVisibility(View.VISIBLE);
                break;
            default: {
                mViewUp.setVisibility(View.INVISIBLE);
                mViewRight.setVisibility(View.INVISIBLE);
                mViewDown.setVisibility(View.INVISIBLE);
                mViewLeft.setVisibility(View.INVISIBLE);
                break;
            }
        }
    }
    
    private static class FrameData {
        public boolean isValid = false;
        public float x = 0.0f;
        public float y = 0.0f;
    }
}
