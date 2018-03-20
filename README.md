<<<<<<< HEAD
# sparkjava-war-example
Working with the sparkjava war example file, downloaded from the [repository](https://github.com/kliakos/sparkjava-war-example)
Build war with maven and sparkjava framework

Steps:

1. Download a fresh [Tomcat 8 distribution](https://tomcat.apache.org/download-80.cgi)
2. Clone the repository to your local machine
3. Run mvn package
4. Copy the generated `sparkjava-hello-world-1.0.war` to the Tomcat `webapps` folder
5. Start Tomcat by running `bin\startup.bat` (or `bin\startaup.sh` for Linux)

6. Tomcat will automatically deploy the war

7. Open [http://localhost:8080/sparkjava-hello-world-1.0/hello](http://localhost:8080/sparkjava-hello-world-1.0/hello) in your browser
=======

# fingerprint-registration-identification

To create a war file for tomcat use maven command -> mvn package
"mvn package" command with create war file which you can upload with tomcat manager

Make sure you are using java 1.8 or higher as spark java is using lambda expressions

For testing purpose use use the sparkjava configuration file.

This is the fingerprint authentication for android and  other distributed services. Might be this will help you.

>>>>>>> 6f2d59516bdd0ec5c83f174447fe41d40114d4ad
