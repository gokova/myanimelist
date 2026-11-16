package com.gokova.myanimelist.feature.mylist.testing

import com.gokova.myanimelist.feature.mylist.data.work.PendingMutationScheduler

class FakePendingMutationScheduler : PendingMutationScheduler {
    var scheduleCallCount = 0

    override fun scheduleSync() {
        scheduleCallCount++
    }
}
