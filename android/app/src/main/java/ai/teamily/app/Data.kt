package ai.teamily.app

import androidx.annotation.DrawableRes

data class Chat(
    val id: String, val title: String, @DrawableRes val avatar: Int,
    val preview: String, val time: String,
    val isAgent: Boolean = false, val members: Int? = null, val unread: Int = 0,
)

data class Agent(
    val id: String, val name: String, @DrawableRes val avatar: Int,
    val description: String, val working: Boolean, val category: String,
)

data class Message(val fromUser: Boolean, val text: String, val time: String)

object Sample {
    val chats = listOf(
        Chat("webpage", "Webpage Developer", R.drawable.agent_webpage, "Landing page draft is ready for review", "2m", isAgent = true, unread = 2),
        Chat("design", "Design Team", R.drawable.user_alice, "Alice: the new M3 tokens are in", "12m", members = 5, unread = 1),
        Chat("slides", "Slides Assistant", R.drawable.agent_slides, "Deck outline done — 12 slides", "1h", isAgent = true),
        Chat("alex", "Alex", R.drawable.user_alex, "Sounds good, see you tomorrow", "3h"),
        Chat("market", "Market Research", R.drawable.agent_market, "Brief with 14 citations attached", "Yesterday", isAgent = true),
        Chat("accounting", "Accounting Assistant", R.drawable.agent_accounting, "March invoices triaged", "Yesterday", isAgent = true),
        Chat("david", "David", R.drawable.user_david, "Sent the invoice", "Mon"),
        Chat("launch", "Launch Crew", R.drawable.user_alice, "David: pushing the build now", "Mon", members = 8),
    )

    val agents = listOf(
        Agent("webpage", "Webpage Developer", R.drawable.agent_webpage, "Builds and publishes landing pages, docs and dashboards from a single prompt.", true, "Web"),
        Agent("slides", "Slides Assistant", R.drawable.agent_slides, "Turns notes into a clean presentation in a handful of prompts.", false, "Slides"),
        Agent("market", "Market Research", R.drawable.agent_market, "Cited briefs on markets, competitors and trends.", true, "Research"),
        Agent("accounting", "Accounting Assistant", R.drawable.agent_accounting, "Invoice triage, expense categories and monthly summaries.", false, "Finance"),
    )

    val thread = listOf(
        Message(true, "Draft a landing page for our AI meetup in Palo Alto — dark hero, big CTA.", "9:38 AM"),
        Message(false, "On it. I'll use the Teamily green (#087c45) so it stays on-brand.", "9:38 AM"),
    )

    fun agentReply(prompt: String) = "Got it — working on \"${prompt.take(60)}\". I'll share a draft shortly."
}
