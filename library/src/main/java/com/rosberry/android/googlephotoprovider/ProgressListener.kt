/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.googlephotoprovider

/**
 * @author Alexei Korshun on 2019-11-19.
 */
interface ProgressListener {

    fun update(progress: Long, done: Boolean)
}