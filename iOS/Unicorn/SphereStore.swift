import SwiftUI
import AppCore

struct SphereStoreKey: EnvironmentKey {
  static var defaultValue: SphereStore? = nil
}

extension EnvironmentValues {
  var sphereStore: SphereStore? {
    get { self[SphereStoreKey.self] }
    set { self[SphereStoreKey.self] = newValue }
  }
}

enum SphereStoreBuilder {
  static func makeStore(gitHubAccessToken token: String) -> SphereStore {
    let httpClient = AppCore.AgentHTTPClientImpl(defaultHeaders: [:], delegate: nil)
    httpClient.setEnvironment(
      environment: AppEnvironment(
        apiBaseUrl: "https://api.github.com",
        apiGqlWebSocketUrl: ""
      )
    )
    httpClient.setAuthToken(authToken: token)

    let logger = AppCore.Logger(
      isErrorEnabled: true,
      backend: AppCoreLoggingBackend()
    )
    let database = DefaultSqlDatabaseProvider(
      logger: logger,
      taskRunner: EmptyBackgroundTaskRunner()
    )
    let builder = AppCore.SphereStoreBuilder(
      storeActorBuilders: [
        Backend0StoreActorsBuilder(
          httpClient: httpClient,
          logger: logger
        )
      ],
      sqlDatabaseProvider: database,
      preferences: AppCorePreferenceStore(),
      connectivityMonitor: httpClient,
      logger: logger
    )

    return builder.makeStore(gitHubAccessToken: token)
  }
}
