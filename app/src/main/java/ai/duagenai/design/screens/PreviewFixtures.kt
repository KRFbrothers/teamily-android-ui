package ai.duagenai.design.screens

/* ============================================================================
 *  PREVIEW / TEST FIXTURES — NOT PRODUCTION DATA
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  `demoArtifacts` and `sampleMemories` used to be the DEFAULT seed of the
 *  in-memory data sources, which meant a production build shipped eight fake
 *  artifacts and five fake memories as if they were the user's real data. Worse,
 *  the nav host's signed-out path fell back to them, so a user who had never
 *  signed in saw a fabricated account.
 *
 *  They are now:
 *    • NOT a default anywhere — `InMemoryArtifactsDataSource` and
 *      `InMemoryMemoryDataSource` default to `emptyList()`;
 *    • NOT reachable from the nav host — the signed-out state is an honest empty
 *      list;
 *    • referenced ONLY by the `@Preview` functions and by tests, which pass them
 *      explicitly.
 *
 *  They live in their own file so that "is this shipped data?" has one obvious
 *  answer: everything in this file is a fixture. Nothing here is loaded unless a
 *  caller names it.
 * ==========================================================================*/

/**
 * Sample artifacts for previews and tests.
 *
 * Written to the product model — files, pages and decks Dua and Agent would
 * actually have produced — so a preview looks like a real account. Never seeded
 * by default.
 */
val demoArtifacts: List<ArtifactItem> = listOf(
    ArtifactItem(
        id = "art_q3_deck",
        title = "Q3 Launch Deck",
        type = "Webpage",
        timestamp = "Aaj",
        sizeBytes = 2_411_724L,
        url = "https://static.teamily.ai/sites/demo/q3-launch-deck/index.html",
    ),
    ArtifactItem(
        id = "art_competitor_brief",
        title = "Competitor Brief",
        type = "Document",
        timestamp = "Aaj",
        sizeBytes = 412_160L,
        url = "https://static.teamily.ai/sites/demo/competitor-brief.pdf",
    ),
    ArtifactItem(
        id = "art_brand_hero",
        title = "Brand Hero Banner",
        type = "Image",
        timestamp = "2 din pehle",
        sizeBytes = 1_887_437L,
        url = "https://static.teamily.ai/sites/demo/brand-hero.png",
    ),
    ArtifactItem(
        id = "art_invoice_2211",
        title = "Invoice #2211",
        type = "Document",
        timestamp = "2 din pehle",
        sizeBytes = 96_256L,
        url = "https://static.teamily.ai/sites/demo/invoice-2211.pdf",
    ),
    ArtifactItem(
        id = "art_demo_reel",
        title = "Product Demo Reel",
        type = "Video",
        timestamp = "4 din pehle",
        sizeBytes = 18_350_080L,
        url = "https://static.teamily.ai/sites/demo/product-demo-reel.mp4",
    ),
    ArtifactItem(
        id = "art_landing_page",
        title = "Landing Page v2",
        type = "Webpage",
        timestamp = "1 hafta pehle",
        sizeBytes = 743_424L,
        url = "https://static.teamily.ai/sites/demo/landing-page-v2/index.html",
    ),
    ArtifactItem(
        id = "art_market_chart",
        title = "Market Share Chart",
        type = "Image",
        timestamp = "1 hafta pehle",
        sizeBytes = 204_800L,
        url = "https://static.teamily.ai/sites/demo/market-share-chart.png",
    ),
    // Deliberately has NO url: it is the artifact that makes the context menu's
    // disabled "Open in new tab" state reachable and testable.
    ArtifactItem(
        id = "art_meeting_notes",
        title = "Meeting Notes",
        type = "Document",
        timestamp = "2 hafte pehle",
        sizeBytes = 28_672L,
        url = null,
    ),
)

/**
 * Sample memories for previews and tests.
 *
 * Never seeded by default — see the file header.
 */
internal val sampleMemories: List<MemoryItem> = listOf(
    MemoryItem(
        id = "mem_launch_q3",
        title = "Q3 launch is the current priority",
        summary = "Deck, competitor research and invoice reconciliation are split across three agents.",
        timestamp = "2 din pehle",
        category = "Project",
        // Has a URL: the long-press "Open" hands it to the shared UrlOpener.
        url = "https://static.teamily.ai/sites/demo/q3-launch-brief/index.html",
    ),
    MemoryItem(
        id = "mem_brand_green",
        title = "Brand green is #087C45",
        summary = "Prefers the Dua Gen AI green with the mint accent — never purple or blue.",
        timestamp = "4 din pehle",
        category = "Preference",
        // Deliberately has NO url: it is the memory that makes the long-press
        // "Open" fall back to the detail dialog, so that path is reachable and
        // testable rather than theoretical.
        url = null,
    ),
    MemoryItem(
        id = "mem_deck_style",
        title = "Decks: 12 slides, brand theme",
        summary = "Slides Assistant should always export with the brand theme applied.",
        timestamp = "1 hafta pehle",
        category = "Preference",
        url = "https://static.teamily.ai/sites/demo/brand-theme-guide.pdf",
    ),
    MemoryItem(
        id = "mem_market_brief",
        title = "Competitor brief needs cited sources",
        summary = "Market Research must cite every claim; eight sources was the last accepted count.",
        timestamp = "1 hafta pehle",
        category = "Project",
        url = "https://static.teamily.ai/sites/demo/competitor-brief.pdf",
    ),
    MemoryItem(
        id = "mem_invoice_2211",
        title = "Invoice #2211 flagged for review",
        summary = "Accounting Assistant is waiting on a human decision before it reconciles this one.",
        timestamp = "2 hafte pehle",
        category = "Finance",
        url = "https://static.teamily.ai/sites/demo/invoice-2211.pdf",
    ),
)
