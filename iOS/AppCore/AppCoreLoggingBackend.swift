public class AppCoreLoggingBackend: NSObject, AppCore.LoggingBackend {
    public func log(level: LoggingLevel, message: String) {
        Swift.print("🟠 [AppCore] \(message)")
    }

    public func log(level: LoggingLevel, exception: KotlinThrowable) {
        print(exception)
    }

    public func logMetric(name: String, value: Int64) {}

    private func print(_ exception: KotlinThrowable) {
        let metadataText = extractMetadata(for: exception)
            .sorted(by: { $0.key < $1.key })
            .map { key, value in
                let body = value.components(separatedBy: .newlines)
                    .map { "  \t\($0)" }
                    .joined(separator: "\n")
                return "\t» \(key)\n\(body)"
            }
            .joined(separator: "\n")

        Swift.print("""
        ⚠️ \(exception.kotlinDescription)
        \(metadataText)
        """)
    }

    private func extractMetadata(for exception: KotlinThrowable) -> [String: String] {
        var metadata: [String: String] = [:]

        // HTTP exceptions
        if let exception = exception as? HTTPError {
            metadata["httpErrorType"] = exception.errorName
            metadata["httpRequest.method"] = String(describing: exception.request.method)
            metadata["httpRequest.resource"] = exception.request.resource.description()
            metadata["httpRequest.body"] = exception.request.body as String?

            if let exception = exception as? HTTPServerError {
                metadata["httpResponse.code"] = String(exception.statusCode.rawValue)
                metadata["httpResponse.body"] = exception.body
            }

            // Early out; We don't need stack trace for HTTPError.
            return metadata
        }

        var cause = exception.cause
        var causeCount = 0
        while let unwrapped = cause {
            cause = unwrapped.cause

            metadata["cause[\(causeCount)].name"] = unwrapped.name
            metadata["cause[\(causeCount)].message"] = unwrapped.message
            metadata["cause[\(causeCount)].stackTrace"] = bridgeArray(unwrapped.getStackTrace())
                .joined(separator: "\n")

            causeCount += 1
        }

        metadata["stackTrace"] = bridgeArray(exception.getStackTrace())
            .joined(separator: "\n")

        return metadata
    }

    private func bridgeArray(_ array: KotlinArray<NSString>) -> [String] {
        var buffer = [String]()
        buffer.reserveCapacity(Int(array.size))
        let iterator = array.iterator()

        while iterator.hasNext(), let next = iterator.next__() as! NSString? {
            buffer.append(String(next))
        }

        return buffer
    }
}
