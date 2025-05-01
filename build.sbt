val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"
val SemanticKernelVersion = "0.9.0"
val CirceVersion = "0.14.6"
val KamonVersion = "2.6.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "arktrio-mesh",
    version := "0.1.0",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      // Core dependencies
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-circe" % PekkoHttpVersion,
      
      // Semantic Kernel for AI integration
      "com.microsoft.semantic-kernel" % "semantic-kernel-core" % SemanticKernelVersion,
      "com.microsoft.semantic-kernel" % "semantic-kernel-connectors" % SemanticKernelVersion,
      
      // JSON handling
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      
      // Monitoring
      "io.kamon" %% "kamon-core" % KamonVersion,
      "io.kamon" %% "kamon-pekko" % KamonVersion,
      "io.kamon" %% "kamon-prometheus" % KamonVersion,
      
      // Testing
      "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    )
  ) 