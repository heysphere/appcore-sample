import SwiftUI
import AppCore

struct NotificationList: View {
  var body: some View {
    List(0 ..< 1024) { _ in
      NotificationRow()
    }
  }
}

struct NotificationList_Previews: PreviewProvider {
  static var previews: some View {
    NotificationList()
  }
}
