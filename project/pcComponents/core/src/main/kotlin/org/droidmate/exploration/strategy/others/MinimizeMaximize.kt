package org.droidmate.exploration.strategy.others

import org.droidmate.exploration.actions.AbstractExplorationAction
import org.droidmate.exploration.actions.MinimizeMaximizeExplorationAction
import org.droidmate.exploration.strategy.AbstractStrategy

class MinimizeMaximize : AbstractStrategy() {
	override fun mustPerformMoreActions(): Boolean = false

	override fun internalDecide(): AbstractExplorationAction {
		return MinimizeMaximizeExplorationAction()
	}
}