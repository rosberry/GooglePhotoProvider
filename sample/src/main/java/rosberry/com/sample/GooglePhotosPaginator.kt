/*
 *
 *  * Copyright (c) 2020 Rosberry. All rights reserved.
 *
 */

package rosberry.com.sample

/**
 * @author Evgeniy Nagibin on 29/04/2020.
 */

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import rosberry.com.sample.entity.Media
import rosberry.com.sample.entity.MediaPage

class GooglePhotosPaginator(
        private val requestFactory: (Int, String?) -> Single<MediaPage>,
        private val viewController: ViewController<Media>
) {

    interface ViewController<T> {
        fun showError(show: Boolean)
        fun showEmptyView(show: Boolean)
        fun showData(data: List<T> = emptyList())
        fun showPageProgress(show: Boolean)
    }

    private var token: String? = null

    private var currentState: State = Empty()
    private val currentData = mutableListOf<Media>()
    private var disposable: Disposable? = null

    //We need use more than 10 for stagged layout manager, because scroll listener works isn't correct.
    private val itemsPerPage = 30

    fun restart() {
        token = ""
        currentData.clear()
        currentState.restart()
    }

    fun loadNewPage() {
        currentState.loadNewPage()
    }

    fun release() {
        currentState.release()
    }

    private fun loadData() {
        disposable?.dispose()
        disposable = requestFactory(itemsPerPage, token)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { newPage -> token = newPage.nextPageToken }
            // same times google photos return duplicated items for different page.
            .map { itemList -> itemList.mediaList.filterDuplicates() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    { items -> currentState.newData(items) },
                    { error -> currentState.fail(error) }
            )
    }

    private fun List<Media>.filterDuplicates(): List<Media> {
        return this.filterNot { mediaItem -> currentData.any { item -> item.id == mediaItem.id } }
    }

    private interface State {
        fun restart() {}
        fun loadNewPage() {}
        fun release() {}
        fun newData(data: List<Media>) {}
        fun fail(error: Throwable) {}
    }

    private inner class Empty : State {

        override fun restart() {
            currentState = EmptyProgress()
            viewController.showPageProgress(true)
            loadData()
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class EmptyProgress : State {

        override fun restart() {
            loadData()
        }

        override fun newData(data: List<Media>) {
            when {
                token.isNullOrBlank().not() -> {
                    currentState = Data()
                    currentData.clear()
                    currentData.addAll(data)
                    viewController.showData(currentData.toList())
                }
                data.isNotEmpty() -> {
                    currentState = AllData()
                    currentData.clear()
                    currentData.addAll(data)
                    viewController.showData(currentData.toList())
                }
                else -> {
                    currentState = EmptyData()
                    viewController.showEmptyView(true)
                }
            }
        }

        override fun fail(error: Throwable) {
            currentState = EmptyError()
            viewController.showPageProgress(false)
            viewController.showError(true)
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class EmptyError : State {

        override fun loadNewPage() {
            currentState = EmptyProgress()
            viewController.showError(false)
            viewController.showPageProgress(true)
            loadData()
        }

        override fun restart() {
            currentState = EmptyProgress()
            viewController.showError(false)
            viewController.showPageProgress(true)
            loadData()
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class EmptyData : State {

        override fun restart() {
            currentState = EmptyProgress()
            viewController.showEmptyView(false)
            viewController.showPageProgress(true)
            loadData()
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class Data : State {

        override fun restart() {
            currentState = EmptyProgress()
            currentData.clear()
            viewController.showData(emptyList())
            viewController.showPageProgress(true)
            loadData()
        }

        override fun loadNewPage() {
            currentState = PageProgress()
            viewController.showPageProgress(true)
            loadData()
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class PageProgress : State {

        override fun restart() {
            currentState = EmptyProgress()
            viewController.showData(emptyList())
            viewController.showPageProgress(true)
            loadData()
        }

        override fun newData(data: List<Media>) {
            if (token.isNullOrBlank().not()) {
                currentState = Data()
                currentData.addAll(data)
                viewController.showData(currentData.toList())
            } else {
                currentState = AllData()
                currentData.addAll(data)
                viewController.showData(currentData.toList())
                viewController.showPageProgress(false)
            }
        }

        override fun fail(error: Throwable) {
            currentState = Data()
            viewController.showPageProgress(false)
            viewController.showError(true)
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class AllData : State {

        override fun restart() {
            currentState = EmptyProgress()
            currentData.clear()
            viewController.showData(emptyList())
            viewController.showPageProgress(true)
            loadData()
        }

        override fun release() {
            currentState = Released()
            disposable?.dispose()
        }
    }

    private inner class Released : State
}