import SwiftUI

struct ChannelListView: View {
    let channels: [Channel]
    @Binding var selectedChannel: Channel?
    let onChannelSelected: (Channel) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(channels) { channel in
                    ChannelButton(
                        channel: channel,
                        isSelected: selectedChannel?.id == channel.id,
                        onTap: {
                            if selectedChannel?.id == channel.id {
                                selectedChannel = nil
                            } else {
                                onChannelSelected(channel)
                            }
                        }
                    )
                }
            }
            .padding()
        }
        .background(Color.black.opacity(0.3))
    }
}

struct ChannelButton: View {
    let channel: Channel
    let isSelected: Bool
    let onTap: () -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        Button(action: onTap) {
            Text(channel.displayName)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 60)
                .background(channel.brandColor)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(isSelected ? Color.white : Color.clear, lineWidth: 4)
                )
                .scaleEffect(isFocused ? 1.05 : 1.0)
                .animation(.easeInOut(duration: 0.2), value: isFocused)
        }
        .buttonStyle(.plain)
        .focused($isFocused)
    }
}

#Preview {
    ChannelListView(
        channels: [
            Channel(name: "ARD", displayName: "ARD", brandColor: .blue),
            Channel(name: "ZDF", displayName: "ZDF", brandColor: .orange)
        ],
        selectedChannel: .constant(nil),
        onChannelSelected: { _ in }
    )
    .frame(width: 300)
}
