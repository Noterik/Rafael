Rafael
======

Media Fragment Server

1) Check out Rafael in Eclipse
2) Adjust the config.xml to match your local video path
3) Build a war using the 'deploy-war' task with the provided build.xml
4) Deploy the war on a Tomcat server

After these steps you can access your videos like this:

http://[yourhost]:[yourport]/rafael/video.mp4?t=0,10

This will give you a media fragment that contains the first 10 seconds from the file video.mp4 
