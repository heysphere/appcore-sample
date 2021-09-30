import UIKit

extension UIApplication: BackgroundTaskRunner {
    public func beginTask(name: String) -> UInt64 {
        UInt64(beginBackgroundTask(withName: name, expirationHandler: nil).rawValue)
    }

    public func endTask(identifier: UInt64) {
        endBackgroundTask(UIBackgroundTaskIdentifier(rawValue: Int(identifier)))
    }
}
