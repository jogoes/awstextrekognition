## Testing AWS Rekognition 

AWS Rekognition can be used to detect in images. 
The code in here is a quick Scala-based implementation used to test the service and the text detection accuracy with different images.

This tool is reading images from a given path. In addition, it generates images containing text rotated with 
different angles to test the accuracy with such text.

Before running the code make sure AWS is configured properly on your machine.

When running the implementation the required first argument is the path to a folder containing images. 
E.g.
```
sbt "run <path to images>"
```

After the run is completed, a new directory named `report` is created in this folder which contains
a HTML report (`index.html`) showing the images and the detected text together with the corresponding 
confidence as reported by AWS Rekognition.
 
 