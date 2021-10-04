final public class AppCorePreferenceStore: NSObject, PreferenceStore {
    public func get(key: String, completionHandler: @escaping (String?, Error?) -> Void) {
        let string = UserDefaults.standard.string(forKey: key)
        completionHandler(string, nil)
    }

    public func set(key: String, value: String?, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        UserDefaults.standard.set(value, forKey: key)
        completionHandler(KotlinUnit(), nil)
    }
}
