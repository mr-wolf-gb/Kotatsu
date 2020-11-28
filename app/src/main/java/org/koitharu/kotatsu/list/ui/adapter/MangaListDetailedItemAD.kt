package org.koitharu.kotatsu.list.ui.adapter

import coil.ImageLoader
import coil.request.Disposable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import kotlinx.android.synthetic.main.item_manga_list_details.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.list.ui.model.MangaListDetailedModel
import org.koitharu.kotatsu.utils.ext.enqueueWith
import org.koitharu.kotatsu.utils.ext.newImageRequest
import org.koitharu.kotatsu.utils.ext.textAndVisible

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	clickListener: OnListItemClickListener<Manga>
) = adapterDelegateLayoutContainer<MangaListDetailedModel, Any>(R.layout.item_manga_list_details) {

	var imageRequest: Disposable? = null

	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}
	itemView.setOnLongClickListener {
		clickListener.onItemLongClick(item.manga, it)
	}

	bind {
		imageRequest?.dispose()
		textView_title.text = item.title
		textView_subtitle.textAndVisible = item.subtitle
		imageRequest = imageView_cover.newImageRequest(item.coverUrl)
			.placeholder(R.drawable.ic_placeholder)
			.fallback(R.drawable.ic_placeholder)
			.error(R.drawable.ic_placeholder)
			.enqueueWith(coil)
		textView_rating.textAndVisible = item.rating
		textView_tags.text = item.tags
	}

	onViewRecycled {
		imageRequest?.dispose()
		imageView_cover.setImageDrawable(null)
	}
}