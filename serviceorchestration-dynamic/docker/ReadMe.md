**Expected folder structure:**

```
root-folder
	└── ah5-common-java-spring/
	└── ah5-core-java-spring/
```

# BUILD IMAGES

1) Update the version numbers in the system Dockerfile and entrypoint.sh.

2) Run the system build command from the _root-folder_: `docker build -f ./ah5-core-java-spring/serviceorchestration-dynamic/docker/Dockerfile-DynamicServiceOrchestration -t arrowhead-serviceorchestration-dynamic:5.0.0 .`

3) Run the database build command from the _root-folder_: `docker build -f ./ah5-core-java-spring/serviceorchestration-dynamic/docker/Dockerfile-DynamicServiceOrchestration-DB -t arrowhead-serviceorchestration-dynamic-db:5.0.0 .`