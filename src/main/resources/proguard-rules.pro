# Keep Spring Boot main application class
-keep class me.xap3y.space.** {
    public static void main(java.lang.String[]);
}


# Keep Spring Boot annotations and configuration classes
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keep @org.springframework.stereotype.Component class *
-keep @org.springframework.boot.autoconfigure.SpringBootApplication class *
-keep @org.springframework.boot.loader.launch.JarLauncher class *
-keep @org.springframework.context.annotation.Configuration class *

# Keep Spring Boot logging classes
-keep class org.apache.logging.log4j.** { *; }
-keep class org.slf4j.** { *; }

# Keep JAXB classes (if needed)
-keep class javax.xml.bind.** { *; }
-keepclassmembers class * {
    @javax.xml.bind.annotation.XmlElement *;
    @javax.xml.bind.annotation.XmlAttribute *;
}

# Optimize the JAR file
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep serialization compatibility
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    private void readObjectNoData();
}

# Exclude META-INF files
-ignorewarnings
-keepdirectories META-INF/**

# Remove debugging information (optional, for further size reduction)
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses