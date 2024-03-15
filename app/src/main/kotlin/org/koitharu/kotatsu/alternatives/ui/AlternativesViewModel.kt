package org.koitharu.kotatsu.alternatives.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.runningFold
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.domain.AlternativesUseCase
import org.koitharu.kotatsu.alternatives.domain.MigrateUseCase
import org.koitharu.kotatsu.core.model.chaptersCount
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.parser.MangaIntent
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.list.domain.ListExtraProvider
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class AlternativesViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val alternativesUseCase: AlternativesUseCase,
	private val migrateUseCase: MigrateUseCase,
	private val extraProvider: ListExtraProvider,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(MangaIntent.KEY_MANGA).manga

	val onMigrated = MutableEventFlow<Manga>()
	val content = MutableStateFlow<List<ListModel>>(listOf(LoadingState))
	private var migrationJob: Job? = null

	init {
		launchJob(Dispatchers.Default) {
			val ref = mangaRepositoryFactory.create(manga.source).getDetails(manga)
			val refCount = ref.chaptersCount()
			alternativesUseCase(manga)
				.map {
					MangaAlternativeModel(
						manga = it,
						progress = extraProvider.getProgress(it.id),
						referenceChapters = refCount,
					)
				}.runningFold<MangaAlternativeModel, List<ListModel>>(listOf(LoadingState)) { acc, item ->
					acc.filterIsInstance<MangaAlternativeModel>() + item + LoadingFooter()
				}.onEmpty {
					emit(
						listOf(
							EmptyState(
								icon = R.drawable.ic_empty_common,
								textPrimary = R.string.nothing_found,
								textSecondary = R.string.text_search_holder_secondary,
								actionStringRes = 0,
							),
						),
					)
				}.collect {
					content.value = it
				}
		}
	}

	fun migrate(target: Manga) {
		if (migrationJob?.isActive == true) {
			return
		}
		migrationJob = launchLoadingJob(Dispatchers.Default) {
			migrateUseCase(manga, target)
			onMigrated.call(target)
		}
	}

	private suspend fun mapList(list: List<Manga>, refCount: Int): List<MangaAlternativeModel> {
		return list.map {
			MangaAlternativeModel(
				manga = it,
				progress = extraProvider.getProgress(it.id),
				referenceChapters = refCount,
			)
		}
	}
}