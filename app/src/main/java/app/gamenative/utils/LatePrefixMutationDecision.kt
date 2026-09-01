package app.gamenative.utils

import com.winlator.container.ContainerData
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LatePrefixMutationDecision(
    val requestId: String,
    val appId: String,
    val containerId: String,
    val mutationPlan: PrefixMutationPlan,
    val requestedContainerData: ContainerData,
    val compatibility: ContainerCompatibilityAnalyzer.Result,
) {
    val fingerprint: String = listOf(containerId, mutationPlan.reasons.sorted().joinToString("|"), compatibility.toString())
        .joinToString("#").hashCode().toUInt().toString(16)
}

enum class LatePrefixMutationChoice { RETAIN_SHARED_BASE, CREATE_NEW, CANCEL }
data class LatePrefixMutationOutcome(val choice: LatePrefixMutationChoice, val fingerprint: String)

/** One pending request at a time; approval is consumed by exactly one matching launch. */
object LatePrefixMutationDecisionManager {
    private data class Pending(val decision: LatePrefixMutationDecision, val result: CompletableDeferred<LatePrefixMutationChoice>)
    private val state = MutableStateFlow<LatePrefixMutationDecision?>(null)
    val decisions = state.asStateFlow()
    private var pending: Pending? = null
    private val oneShotApprovals = mutableSetOf<String>()

    suspend fun request(
        appId: String,
        containerId: String,
        plan: PrefixMutationPlan,
        requested: ContainerData,
        compatibility: ContainerCompatibilityAnalyzer.Result,
    ): LatePrefixMutationOutcome {
        val decision = LatePrefixMutationDecision(UUID.randomUUID().toString(), appId, containerId, plan, requested, compatibility)
        synchronized(this) {
            check(pending == null) { "Another late prefix decision is already pending" }
            val deferred = CompletableDeferred<LatePrefixMutationChoice>()
            pending = Pending(decision, deferred)
            state.value = decision
        }
        return try {
            LatePrefixMutationOutcome(pending!!.result.await(), decision.fingerprint)
        } finally {
            synchronized(this) {
                if (pending?.decision?.requestId == decision.requestId) pending = null
                if (state.value?.requestId == decision.requestId) state.value = null
            }
        }
    }

    fun resolve(requestId: String, choice: LatePrefixMutationChoice) {
        synchronized(this) {
            val current = pending?.takeIf { it.decision.requestId == requestId } ?: return
            if (choice == LatePrefixMutationChoice.RETAIN_SHARED_BASE) oneShotApprovals += current.decision.fingerprint
            current.result.complete(choice)
            state.value = null
        }
    }

    fun consumeApproval(fingerprint: String): Boolean = synchronized(this) { oneShotApprovals.remove(fingerprint) }

    fun cancelPending() = synchronized(this) {
        pending?.result?.complete(LatePrefixMutationChoice.CANCEL)
        state.value = null
    }

    internal fun resetForTests() = synchronized(this) {
        pending?.result?.cancel()
        pending = null
        state.value = null
        oneShotApprovals.clear()
    }
}
