val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"
val SemanticKernelVersion = "0.9.0"
val CirceVersion = "0.14.6"
val KamonVersion = "2.6.0"
val PekkoGrpcVersion = "1.0.0"
val ScalaPbVersion = "0.11.15"

lazy val common = project
  .in(file("arktrio/common"))
  .settings(
    name := "arktrio-common",
    version := "0.3.5.0",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-circe" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion,
      "org.apache.pekko" %% "pekko-grpc-runtime" % PekkoGrpcVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "com.google.protobuf" % "protobuf-java" % "3.25.3",
      "com.thesamet.scalapb" %% "scalapb-runtime" % ScalaPbVersion % "protobuf"
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    Compile / PB.protoSources += baseDirectory.value / "src" / "main" / "protobuf"
  )
  .enablePlugins(PekkoGrpcPlugin)

lazy val center = project
  .in(file("arktrio/center"))
  .dependsOn(common)
  .settings(
    name := "arktrio-center",
    version := "0.3.5.0",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-circe" % PekkoHttpVersion,
      "com.microsoft.semantic-kernel" % "semantic-kernel-core" % SemanticKernelVersion,
      "com.microsoft.semantic-kernel" % "semantic-kernel-connectors" % SemanticKernelVersion,
      "io.kamon" %% "kamon-core" % KamonVersion,
      "io.kamon" %% "kamon-pekko" % KamonVersion,
      "io.kamon" %% "kamon-prometheus" % KamonVersion,
      "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    )
  )

lazy val edge = project
  .in(file("arktrio/edge"))
  .dependsOn(common)
  .settings(
    name := "arktrio-edge",
    version := "0.3.5.0",
    scalaVersion := "3.3.3",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-circe" % PekkoHttpVersion,
      "io.kamon" %% "kamon-core" % KamonVersion,
      "io.kamon" %% "kamon-pekko" % KamonVersion,
      "io.kamon" %% "kamon-prometheus" % KamonVersion,
      "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    )
  )

lazy val root = project
  .in(file("."))
  .aggregate(common, center, edge)
  .settings(
    name := "arktrio",
    version := "0.3.5.0",
    scalaVersion := "3.3.3"
  ) 