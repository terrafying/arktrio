package arktrio.common.util

object Constants {
  val ProjectName = "arktrio"
  val ProjectVersion = "0.1.0"
  
  object Ports {
    val Center = 2236
    val Edge = 2237
  }
  
  object Environment {
    val CenterHost = "ARKTRIO_CENTER_STATIC_HOST"
    val CenterPort = "ARKTRIO_CENTER_STATIC_PORT"
    val EdgeHost = "ARKTRIO_EDGE_STATIC_HOST"
    val EdgePort = "ARKTRIO_EDGE_STATIC_PORT"
    val EdgeIdPrefix = "ARKTRIO_EDGE_STATIC_EDGE_ID_PREFIX"
    val EdgeLogLevel = "ARKTRIO_EDGE_STATIC_LOG_LEVEL"
    val EdgeLogLevelColor = "ARKTRIO_EDGE_STATIC_LOG_LEVEL_COLOR"
    val EdgeGrpcClientTls = "ARKTRIO_EDGE_GRPC_CLIENT_TLS"
  }
  
  object Config {
    val DefaultEdgeIdPrefix = "edge"
    val DefaultHost = "0.0.0.0"
    val DefaultLogLevel = "Info"
    val DefaultLogLevelColor = true
    val DefaultGrpcClientTls = false
  }
} 