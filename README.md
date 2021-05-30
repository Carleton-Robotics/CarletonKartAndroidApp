# CarletonKartAndroidApp
In this app there are three activities that use the OpenCV Library in different ways. All can be accessed using buttons in the main activity.
The ColorBlob activity uses object detection to trace lines around objects that are pressed on the screen.
The LaneDetector uses Hough Lines to draw edges on objects in the frame.
The HumanDetector activity uses the Darknet YOLO Neural Net to detect what the objects in front of the camera are, like a person, car, etc.

TODO: All of these activities work, but they don't function particulary well. ColorBlob and LaneDetection draw many superfluous lines. And the HumanDetector has a hard time detection in real time because the computations take so long. 
