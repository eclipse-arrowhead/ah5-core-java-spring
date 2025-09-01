**Expected folder structure:**

```
root-folder
	└── ah5-common-java-spring/
	└── ah5-core-java-spring/
```

# BUILD IMAGES

1) Update the version numbers in the system Dockerfile and entrypoint.sh.

2) Run the system build command from the _root-folder_: `docker build -f ./ah5-core-java-spring/serviceregistry/docker/Dockerfile-ServiceRegistry -t arrowhead-serviceregistry:5.0.0 .`

3) Run the database build command from the _root-folder_: `docker build -f ./ah5-core-java-spring/serviceregistry/docker/Dockerfile-ServiceRegistry-DB -t arrowhead-serviceregistry-db:5.0.0 .`