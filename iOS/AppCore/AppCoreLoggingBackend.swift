public class AppCoreLoggingBackend: NSObject, AppCore.LoggingBackend {
    public func log(level: LoggingLevel, message: String) {
        // TODO
    }

    public func log(level: LoggingLevel, exception: KotlinThrowable) {
        // TODO
    }

    public func logMetric(name: String, value: Int64) {
        // TODO
    }
}
