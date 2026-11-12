package com.gokova.myanimelist.feature.franchise.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gokova.myanimelist.core.ui.theme.spacing
import com.gokova.myanimelist.feature.franchise.R
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseNode

@Composable
fun FranchiseListView(
    graph: FranchiseGraph,
    onNodeClick: (FranchiseNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSideWorks =
        graph.sideWorksByParent.values
            .flatten()
            .distinctBy { it.animeId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        if (graph.mainlineSequence.isNotEmpty()) {
            mainlineSection(graph.mainlineSequence, onNodeClick)
        }
        if (allSideWorks.isNotEmpty()) {
            sideWorksSection(allSideWorks, onNodeClick)
        }
    }
}

private fun LazyListScope.mainlineSection(
    mainline: List<FranchiseNode>,
    onNodeClick: (FranchiseNode) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.franchise_list_mainline_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.extraSmall),
        )
    }

    items(mainline, key = { it.animeId }) { node ->
        FranchiseNodeCard(
            node = node,
            onClick = onNodeClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(vertical = 4.dp),
        )
    }
}

private fun LazyListScope.sideWorksSection(
    sideWorks: List<FranchiseNode>,
    onNodeClick: (FranchiseNode) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.franchise_list_side_works_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = MaterialTheme.spacing.medium, bottom = 4.dp),
        )
    }

    items(sideWorks, key = { it.animeId }) { node ->
        FranchiseNodeCard(
            node = node,
            onClick = onNodeClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(vertical = 4.dp),
        )
    }
}
