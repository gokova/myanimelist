package com.gokova.myanimelist.feature.franchise.domain.model

enum class FranchiseEdgeType {
    SEQUEL,
    PREQUEL,
    SIDE_STORY,
    SPIN_OFF,
    ALTERNATIVE_VERSION,
    ALTERNATIVE_SETTING,
    PARENT_STORY,
    FULL_STORY,
    OTHER,
    ;

    companion object {
        fun fromRelationType(relationType: String): FranchiseEdgeType =
            when (relationType.lowercase()) {
                "sequel" -> SEQUEL
                "prequel" -> PREQUEL
                "side_story" -> SIDE_STORY
                "spin_off" -> SPIN_OFF
                "alternative_version" -> ALTERNATIVE_VERSION
                "alternative_setting" -> ALTERNATIVE_SETTING
                "parent_story" -> PARENT_STORY
                "full_story" -> FULL_STORY
                else -> OTHER
            }
    }
}

data class FranchiseEdge(
    val sourceId: Long,
    val targetId: Long,
    val edgeType: FranchiseEdgeType,
    val label: String,
)
