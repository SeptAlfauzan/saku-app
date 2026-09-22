import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        if let dsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            SentryInitKt.doInitSentry(dsn: dsn)
        }
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}