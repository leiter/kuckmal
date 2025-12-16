import SwiftUI

struct DetailView: View {
    let mediaItem: MediaItem
    let onDismiss: () -> Void

    @State private var isHighQuality = true
    @FocusState private var focusedButton: ButtonType?

    enum ButtonType {
        case play, dismiss
    }

    var body: some View {
        VStack(spacing: 40) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text(mediaItem.channel)
                        .font(.headline)
                        .foregroundColor(.blue)

                    Text(mediaItem.theme)
                        .font(.title2)
                        .foregroundColor(.secondary)

                    Text(mediaItem.title)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                }

                Spacer()

                // Channel logo placeholder
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 120, height: 80)
                    .overlay(
                        Text(mediaItem.channel)
                            .font(.caption)
                            .foregroundColor(.gray)
                    )
            }
            .padding(.horizontal, 60)

            // Metadata
            HStack(spacing: 40) {
                MetadataItem(icon: "calendar", label: "Datum", value: mediaItem.date)
                MetadataItem(icon: "clock", label: "Zeit", value: mediaItem.time)
                MetadataItem(icon: "timer", label: "Dauer", value: mediaItem.duration)
                MetadataItem(icon: "doc", label: "Groesse", value: mediaItem.size)
            }
            .padding(.horizontal, 60)

            // Description
            Text(mediaItem.description)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.leading)
                .padding(.horizontal, 60)

            Spacer()

            // Quality selection
            HStack(spacing: 20) {
                Text("Qualitaet:")
                    .foregroundColor(.secondary)

                Button(action: { isHighQuality = true }) {
                    HStack {
                        Image(systemName: isHighQuality ? "checkmark.circle.fill" : "circle")
                        Text("HD")
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(isHighQuality ? Color.blue.opacity(0.3) : Color.clear)
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)

                Button(action: { isHighQuality = false }) {
                    HStack {
                        Image(systemName: !isHighQuality ? "checkmark.circle.fill" : "circle")
                        Text("SD")
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(!isHighQuality ? Color.blue.opacity(0.3) : Color.clear)
                    .cornerRadius(8)
                }
                .buttonStyle(.plain)
            }

            // Action buttons
            HStack(spacing: 30) {
                Button(action: {
                    // Play action - in real app would open video player
                    print("Playing: \(mediaItem.title) in \(isHighQuality ? "HD" : "SD")")
                }) {
                    HStack {
                        Image(systemName: "play.fill")
                        Text("Abspielen")
                    }
                    .font(.title3)
                    .padding(.horizontal, 40)
                    .padding(.vertical, 16)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .focused($focusedButton, equals: .play)

                Button(action: onDismiss) {
                    HStack {
                        Image(systemName: "xmark")
                        Text("Schliessen")
                    }
                    .font(.title3)
                    .padding(.horizontal, 40)
                    .padding(.vertical, 16)
                    .background(Color.gray.opacity(0.3))
                    .foregroundColor(.primary)
                    .cornerRadius(12)
                }
                .focused($focusedButton, equals: .dismiss)
            }
            .padding(.bottom, 40)
        }
        .padding(.top, 40)
        .background(Color.black)
        .onAppear {
            focusedButton = .play
        }
    }
}

struct MetadataItem: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.blue)

            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            Text(value)
                .font(.headline)
        }
        .frame(minWidth: 100)
    }
}

#Preview {
    DetailView(
        mediaItem: SampleData.createMediaItem(
            channel: "ARD",
            theme: "Tatort",
            title: "Tatort - Folge 1"
        ),
        onDismiss: {}
    )
}
