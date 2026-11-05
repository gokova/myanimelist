package com.gokova.myanimelist.feature.recommendation.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gokova.myanimelist.feature.recommendation.R
import com.gokova.myanimelist.feature.recommendation.domain.model.DurationFitTier
import com.gokova.myanimelist.feature.recommendation.domain.model.PopularityTier

@Composable
fun DurationFitTier.format(): String =
    when (this) {
        DurationFitTier.SingleRelease ->
            stringResource(R.string.recommendation_why_duration_single)
        is DurationFitTier.ShortSeries ->
            stringResource(R.string.recommendation_why_duration_short, episodes)
        is DurationFitTier.StandardCour ->
            stringResource(R.string.recommendation_why_duration_standard, episodes)
        is DurationFitTier.LongCommitment ->
            stringResource(R.string.recommendation_why_duration_long, episodes)
    }

@Composable
fun PopularityTier.format(): String =
    when (this) {
        PopularityTier.MAINSTREAM_FAVORITE ->
            stringResource(R.string.recommendation_why_popularity_mainstream)
        PopularityTier.POPULAR_PICK ->
            stringResource(R.string.recommendation_why_popularity_popular)
        PopularityTier.MODERATELY_KNOWN ->
            stringResource(R.string.recommendation_why_popularity_moderate)
        PopularityTier.NICHE_DISCOVERY ->
            stringResource(R.string.recommendation_why_popularity_niche)
    }
