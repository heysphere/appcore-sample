import SwiftUI
import AppCore

struct NotificationList: View {
  @State var activeDetail: Int?

  var body: some View {
    List(0 ..< 1024) { item in
      Button { activeDetail = item } label: {
        NotificationRow()
      }
    }
    .navigation($activeDetail) { _ in
      NotificationInfo()
    }
    .navigationTitle(Text("Notifications"))
  }
}

struct NotificationList_Previews: PreviewProvider {
  static var previews: some View {
    NavigationView {
      NotificationList()
    }
  }
}
