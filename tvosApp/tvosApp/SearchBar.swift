import SwiftUI

struct SearchBar: View {
    @Binding var text: String
    let placeholder: String
    let onSearch: (String) -> Void
    let onClear: () -> Void

    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: "magnifyingglass")
                .font(.title2)
                .foregroundColor(.gray)

            TextField(placeholder, text: $text)
                .font(.title3)
                .focused($isFocused)
                .onSubmit {
                    onSearch(text)
                }
                .onChange(of: text) { _, newValue in
                    if newValue.count >= 2 {
                        onSearch(newValue)
                    }
                }

            if !text.isEmpty {
                Button {
                    text = ""
                    onClear()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title2)
                        .foregroundColor(.gray)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.gray.opacity(0.2))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isFocused ? Color.blue : Color.clear, lineWidth: 3)
        )
    }
}

struct SearchResultsView: View {
    let results: [SearchResult]
    let onResultSelected: (SearchResult) -> Void

    var body: some View {
        if results.isEmpty {
            VStack {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 50))
                    .foregroundColor(.gray)
                    .padding(.bottom, 10)
                Text("Keine Ergebnisse")
                    .font(.title3)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(results) { result in
                        Button {
                            onResultSelected(result)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(result.title)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                        .lineLimit(1)

                                    HStack {
                                        Text(result.channel)
                                            .font(.caption)
                                            .foregroundColor(.blue)
                                        Text("•")
                                            .foregroundColor(.gray)
                                        Text(result.theme)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                            .lineLimit(1)
                                    }

                                    Text(result.date)
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .foregroundColor(.gray)
                            }
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(Color.gray.opacity(0.1))
                            )
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding()
            }
        }
    }
}

#Preview {
    VStack {
        SearchBar(
            text: .constant("test"),
            placeholder: "Suchen...",
            onSearch: { _ in },
            onClear: {}
        )
        .padding()

        SearchResultsView(
            results: [
                SearchResult(id: 1, title: "Tagesschau", theme: "Nachrichten", channel: "ARD", date: "25.07.2024"),
                SearchResult(id: 2, title: "heute", theme: "Nachrichten", channel: "ZDF", date: "25.07.2024")
            ],
            onResultSelected: { _ in }
        )
    }
}
