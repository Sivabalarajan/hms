package com.android.hms.ui.fragments

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
    open fun filter(searchText: String) { }
}