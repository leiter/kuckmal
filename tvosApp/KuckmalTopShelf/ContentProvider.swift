import TVServices

class ContentProvider: TVTopShelfContentProvider {

    override func loadTopShelfContent() async -> TVTopShelfContent? {
        // Create sectioned content with two rows
        let recentSection = createRecentSection()
        let popularSection = createPopularSection()

        return TVTopShelfSectionedContent(sections: [recentSection, popularSection])
    }

    private func createRecentSection() -> TVTopShelfItemCollection<TVTopShelfSectionedItem> {
        let items = [
            createItem(
                id: "1",
                title: "Tagesschau",
                channel: "ARD",
                theme: "Nachrichten",
                imageName: "tagesschau"
            ),
            createItem(
                id: "2",
                title: "Terra X - Faszination Erde",
                channel: "ZDF",
                theme: "Terra X",
                imageName: "terrax"
            ),
            createItem(
                id: "3",
                title: "Tatort - Muenster",
                channel: "ARD",
                theme: "Tatort",
                imageName: "tatort"
            ),
            createItem(
                id: "4",
                title: "Kulturzeit",
                channel: "3Sat",
                theme: "Kultur",
                imageName: "kulturzeit"
            ),
            createItem(
                id: "5",
                title: "heute journal",
                channel: "ZDF",
                theme: "Nachrichten",
                imageName: "heute"
            )
        ]

        let collection = TVTopShelfItemCollection(items: items)
        collection.title = "Zuletzt hinzugefuegt"
        return collection
    }

    private func createPopularSection() -> TVTopShelfItemCollection<TVTopShelfSectionedItem> {
        let items = [
            createItem(
                id: "10",
                title: "Sportschau",
                channel: "ARD",
                theme: "Sport",
                imageName: "sportschau"
            ),
            createItem(
                id: "11",
                title: "ARTE Journal",
                channel: "ARTE.DE",
                theme: "Nachrichten",
                imageName: "arte"
            ),
            createItem(
                id: "12",
                title: "nano",
                channel: "3Sat",
                theme: "Wissenschaft",
                imageName: "nano"
            ),
            createItem(
                id: "13",
                title: "extra 3",
                channel: "NDR",
                theme: "Satire",
                imageName: "extra3"
            ),
            createItem(
                id: "14",
                title: "Quarks",
                channel: "WDR",
                theme: "Wissenschaft",
                imageName: "quarks"
            )
        ]

        let collection = TVTopShelfItemCollection(items: items)
        collection.title = "Beliebte Themen"
        return collection
    }

    private func createItem(
        id: String,
        title: String,
        channel: String,
        theme: String,
        imageName: String
    ) -> TVTopShelfSectionedItem {
        let item = TVTopShelfSectionedItem(identifier: id)
        item.title = title

        // Create deep link URL
        var components = URLComponents()
        components.scheme = "kuckmal"
        components.host = "play"
        components.queryItems = [
            URLQueryItem(name: "channel", value: channel),
            URLQueryItem(name: "theme", value: theme),
            URLQueryItem(name: "title", value: title)
        ]

        if let url = components.url {
            item.playAction = TVTopShelfAction(url: url)
            item.displayAction = TVTopShelfAction(url: url)
        }

        // Set image shape for sectioned content
        item.imageShape = .hdtv

        return item
    }
}
