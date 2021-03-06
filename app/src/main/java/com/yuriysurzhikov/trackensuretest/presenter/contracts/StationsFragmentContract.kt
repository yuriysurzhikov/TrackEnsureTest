package com.yuriysurzhikov.trackensuretest.presenter.contracts

import android.content.Context
import com.yuriysurzhikov.trackensuretest.model.entities.Place
import com.yuriysurzhikov.trackensuretest.model.entities.Refueling

interface StationsFragmentContract {
    interface Model {

    }

    interface View {

    }

    interface Presenter {
        fun openAddingActivity(context: Context)
        fun openEditingActivity(refueling: Refueling, place: Place, context: Context)
        fun deleteRefuelingNote(refueling: Refueling)
    }
}